package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.InMemoryStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.math.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default implementation of a structure data provider.
 */
@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private final StructureReader structureReader;
    private final StructureWriter renumberedStructureWriter;
    private final StrucmotifConfig strucmotifConfig;
    private final String dataSource;
    private final Path renumberedDirectory;
    private final String extension;
    private boolean paths;
    private boolean caching;
    // keys must be upper-case
    private Map<String, Structure> structureCache;

    /**
     * Construct a structure provider.
     * @param structureReader the reader
     * @param structureWriter the writer
     * @param strucmotifConfig the config
     */
    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader,
                                     StructureWriter structureWriter,
                                     StrucmotifConfig strucmotifConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = structureWriter;
        this.strucmotifConfig = strucmotifConfig;
        this.dataSource = strucmotifConfig.getDataSource();
        this.renumberedDirectory = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.RENUMBERED_DIRECTORY);
        this.extension = strucmotifConfig.isRenumberedGzip() ? ".bcif.gz" : ".bcif";

        logger.info("BinaryCIF data source is {} - CIF fetch URL: {} - precision: {} - gzipping: {}",
                strucmotifConfig.getDataSource(),
                strucmotifConfig.getCifFetchUrl(),
                strucmotifConfig.getRenumberedCoordinatePrecision(),
                strucmotifConfig.isRenumberedGzip());

        this.paths = false;
        this.caching = false;
    }

    private void ensureRenumberedPathExists() {
        try {
            Files.createDirectories(renumberedDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("Duplicates")
    private String prepareUri(String raw, String structureIdentifier) {
        String pdbId = structureIdentifier.toLowerCase();
        String pdbIdUc = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String middleUc = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", middleUc)
                .replace("{id}", pdbId)
                .replace("{ID}", pdbIdUc);
    }

    private URL getCifFetchUrl(String structureIdentifier) {
        try {
            return new URL(prepareUri(strucmotifConfig.getCifFetchUrl(), structureIdentifier));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getOriginalStructurePath(String structureIdentifier) {
        return Paths.get(prepareUri(dataSource, structureIdentifier));
    }

    private Path getRenumberedStructurePath(String structureIdentifier) {
        return renumberedDirectory.resolve(structureIdentifier + extension);
    }

    private InputStream getRenumberedInputStream(String structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    @PostConstruct
    public void initializeRenumberedStructureCache() throws IOException {
        InMemoryStrategy strategy = strucmotifConfig.getInMemoryStrategy();
        if (strategy == InMemoryStrategy.OFF) {
            logger.info("Structure data will be read from file-system");
            return;
        }

        // TODO add off-heap strategy
        if (strategy == InMemoryStrategy.HEAP) {
            logger.info("Structure data will be kept in memory - start loading...");

            this.caching = true;
            List<Path> files;
            try (Stream<Path> pathStream = Files.walk(renumberedDirectory)) {
                files = pathStream.parallel()
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());
            }
            long start = System.nanoTime();
            this.structureCache = new HashMap<>();

            int loadingChunkSize = strucmotifConfig.getLoadingChunkSize();
            Partition<Path> partitions = new Partition<>(files, loadingChunkSize);
            logger.info("Formed {} partitions of {} structures",
                    partitions.size(),
                    loadingChunkSize);

            for (int i = 0; i < partitions.size(); i++) {
                String partitionContext = (i + 1) + " / " + partitions.size();

                List<Path> partition = partitions.get(i);
                logger.info("[{}] Start loading partition", partitionContext);

                // this will run on strucmotif-instances only: let's ignore thread-parameter
                Map<String, Structure> buffer = partition.parallelStream()
                        .map(this::loadRenumberedStructure)
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

                this.structureCache.putAll(buffer);
            }

            long time = (System.nanoTime() - start) / 1000 / 1000 / 1000;
            long atoms = structureCache.values()
                    .stream()
                    .mapToLong(Structure::getAtomCount)
                    .sum();

            logger.info("Done caching structure data in {} seconds - {} atoms in {} structures held in memory", time, atoms, structureCache.size());
        }
    }

    private Pair<String, Structure> loadRenumberedStructure(Path path) {
        try {
            String pdbId = path.toFile().getName().split("\\.")[0];
            Structure structure = readFromInputStream(Files.newInputStream(path));
            return new Pair<>(pdbId, structure);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream getOriginalInputStream(String structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            if (Files.isReadable(originalPath)) {
                return Files.newInputStream(originalPath);
            } else {
                return getCifFetchUrl(structureIdentifier).openStream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        return structureReader.readFromInputStream(inputStream);
    }

    @Override
    public Structure readRenumbered(String structureIdentifier) {
        if (caching) {
            return structureCache.get(structureIdentifier);
        }
        return readFromInputStream(getRenumberedInputStream(structureIdentifier));
    }

    @Override
    public Structure readOriginal(String structureIdentifier) {
        return readFromInputStream(getOriginalInputStream(structureIdentifier));
    }

    @Override
    public Structure readSome(String structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            return readFromInputStream(Files.newInputStream(originalPath));
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
                if (Files.isReadable(renumberedPath)) {
                    return readFromInputStream(Files.newInputStream(renumberedPath));
                } else {
                    return readFromInputStream(getCifFetchUrl(structureIdentifier).openStream());
                }
            } catch (IOException e2) {
                throw new UncheckedIOException(e2);
            }
        }
    }

    @Override
    public void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile) {
        if (!paths) {
            ensureRenumberedPathExists();
            this.paths = true;
        }
        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }

    @Override
    public void deleteRenumbered(String structureIdentifier) {
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
