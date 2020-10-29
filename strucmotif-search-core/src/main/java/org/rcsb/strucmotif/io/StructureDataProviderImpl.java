package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.write.RenumberedStructureWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private final StructureReader structureReader;
    private final RenumberedStructureWriter renumberedStructureWriter;
    private final MotifSearchConfig motifSearchConfig;
    private final String dataSource;
    private final Path renumberedPath;
    private final String extension;

    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader,
                                     RenumberedStructureWriter renumberedStructureWriter,
                                     MotifSearchConfig motifSearchConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = renumberedStructureWriter;
        this.motifSearchConfig = motifSearchConfig;
        this.dataSource = motifSearchConfig.getDataSource();
        this.renumberedPath = Paths.get(motifSearchConfig.getRootPath())
                .resolve("renumbered");
        this.extension = motifSearchConfig.isRenumberedGzip() ? ".bcif.gz" : ".bcif";

        // ensure directories exist
        try {
            Files.createDirectories(renumberedPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        logger.info("BinaryCIF data source is {} - BCIF fetch URL: {} - precision: {} - gzipping: {}",
                motifSearchConfig.getDataSource(),
                motifSearchConfig.getBcifFetchUrl(),
                motifSearchConfig.getRenumberedCoordinatePrecision(),
                motifSearchConfig.isRenumberedGzip());
    }

    private String prepareUri(String raw, StructureIdentifier structureIdentifier) {
        String pdbId = structureIdentifier.getPdbId().toLowerCase();
        String PDBID = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String MIDDLE = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", MIDDLE)
                .replace("{id}", pdbId)
                .replace("{ID}", PDBID);
    }

    private URL getBcifFetchUrl(StructureIdentifier structureIdentifier) {
        try {
            return new URL(prepareUri(motifSearchConfig.getBcifFetchUrl(), structureIdentifier));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getOriginalStructurePath(StructureIdentifier structureIdentifier) {
        return Paths.get(prepareUri(motifSearchConfig.getDataSource(), structureIdentifier));
    }

    private Path getRenumberedStructurePath(StructureIdentifier structureIdentifier) {
        return renumberedPath.resolve(structureIdentifier.getPdbId().toLowerCase() + extension);
    }

    private InputStream getRenumberedInputStream(StructureIdentifier structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream getOriginalInputStream(StructureIdentifier structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
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
        return readFromInputStream(getRenumberedInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readOriginal(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getOriginalInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readSome(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            return readFromInputStream(Files.newInputStream(originalPath), selection);
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
                return readFromInputStream(Files.newInputStream(renumberedPath), selection);
            } catch (IOException e2) {
                try {
                    return readFromInputStream(getBcifFetchUrl(structureIdentifier).openStream(), selection);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public void writeRenumbered(StructureIdentifier structureIdentifier, MmCifFile mmCifFile) {
        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }

    @Override
    public void deleteRenumbered(StructureIdentifier structureIdentifier) {
        try {
            Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
            if (Files.exists(renumberedPath)) {
                Files.delete(getRenumberedStructurePath(structureIdentifier));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
