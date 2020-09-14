package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.write.RenumberedStructureWriter;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private static final int BUFFER_SIZE = 65536;
    private final StructureReader structureReader;
    private final RenumberedStructureWriter renumberedStructureWriter;
    private final MotifSearchConfig motifSearchConfig;
    private final boolean keepStructuresInMemory;
    private final Map<StructureIdentifier, byte[]> cache;

    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader, RenumberedStructureWriter renumberedStructureWriter, StateRepository stateRepository, MotifSearchConfig motifSearchConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = renumberedStructureWriter;
        this.motifSearchConfig = motifSearchConfig;

        this.keepStructuresInMemory = motifSearchConfig.isKeepStructureInMemory();
        if (keepStructuresInMemory) {
            logger.info("Configured to keep structure data in memory");
            Collection<StructureIdentifier> structureIdentifiers = stateRepository.selectSupported();
            int target = structureIdentifiers.size();
            logger.info("{} valid structures", target);

            AtomicInteger counter = new AtomicInteger();
            cache = structureIdentifiers.parallelStream()
                    .peek(id -> {
                        if (counter.incrementAndGet() % 5000 == 0) {
                            logger.info("[{} / {}] Loading structures into cache",
                                    counter.get(),
                                    target);
                        }
                    })
                    .collect(Collectors.toMap(Function.identity(), this::loadIntoByteArray));
        } else {
            logger.info("Configured to load structure data ad hoc from file-system");
            cache = null;
        }

        boolean dataSourceHealthy;
        try {
            dataSourceHealthy = Files.list(Paths.get(motifSearchConfig.getDataSource()))
                    .anyMatch(path -> path.toFile().getName().endsWith(".bcif.gz"));
        } catch (IOException e) {
            dataSourceHealthy = false;
        }
        logger.info("BinaryCIF data source at {} looks healthy: {}",
                motifSearchConfig.getDataSource(),
                dataSourceHealthy);
    }

    private byte[] loadIntoByteArray(StructureIdentifier structureIdentifier) {
        try {
            InputStream inputStream = new GZIPInputStream(Files.newInputStream(getRenumberedStructurePath(structureIdentifier)), BUFFER_SIZE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            byteArrayOutputStream.flush();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            inputStream.close();

            return byteArray;
        } catch (IOException e) {
            logger.warn("No structure data exists for {} - path: {}",
                    structureIdentifier,
                    getRenumberedStructurePath(structureIdentifier));
            return null;
        }
    }

    private URL getBcifFetchUrl(StructureIdentifier structureIdentifier) {
        try {
            return new URL(String.format(motifSearchConfig.getBcifFetchUrl(), structureIdentifier.getPdbId().toLowerCase()));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getRenumberedStructurePath(StructureIdentifier structureIdentifier) {
        return Paths.get(motifSearchConfig.getRootPath())
                .resolve("renumbered")
                .resolve(structureIdentifier.getPdbId().toLowerCase() + ".bcif.gz");
    }

    private InputStream getRenumberedStructureInputStream(StructureIdentifier structureIdentifier) {
        try {
            if (keepStructuresInMemory) {
                byte[] bytes = cache.get(structureIdentifier);
                if (bytes == null) {
                    throw new IOException("No in-memory structure data for " + structureIdentifier.getPdbId());
                }
                return new ByteArrayInputStream(bytes);
            } else {
                return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream getOriginalStructureInputStream(StructureIdentifier structureIdentifier) {
        try {
            Path originalPath = Paths.get(motifSearchConfig.getDataSource()).resolve(structureIdentifier.getPdbId() + ".bcif.gz");
            if (Files.exists(originalPath)) {
                return Files.newInputStream(originalPath);
            } else {
                return getBcifFetchUrl(structureIdentifier).openStream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection) {
        return structureReader.readFromInputStream(inputStream, selection);
    }

    @Override
    public Structure readRenumbered(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getRenumberedStructureInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readOriginal(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getOriginalStructureInputStream(structureIdentifier), selection);
    }

    @Override
    public void writeRenumbered(StructureIdentifier structureIdentifier, MmCifFile mmCifFile) {
        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }
}
