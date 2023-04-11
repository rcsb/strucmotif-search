package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.ffindex.AppendableFileBundle;
import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.ffindex.WritableFileBundle;
import org.rcsb.strucmotif.config.InMemoryStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.math.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    // 'production' data that can be queried
    private final Path dataPath;
    private final Path indexPath;
    private ReadableFileBundle fileBundle;
    // 'update' data that holds the partial delta of new data (to be merged into production files)
    private final Path partialDataPath;
    private final Path partialIndexPath;
    private AppendableFileBundle partialFileBundle;
    // paths for 'temporary' bundle written when 'production' data is getting modified
    private final Path temporaryDataPath;
    private final Path temporaryIndexPath;
    private final String extension;
    private boolean caching;
    // keys must be upper-case
    private Map<String, Structure> structureCache;

    /**
     * Construct a structure provider.
     * @param structureReader the reader
     * @param structureWriter the writer
     * @param threadPool shared pool for parallel operations
     * @param strucmotifConfig the config
     */
    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader,
                                     StructureWriter structureWriter,
                                     ThreadPool threadPool,
                                     StrucmotifConfig strucmotifConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = structureWriter;
        this.strucmotifConfig = strucmotifConfig;
        this.dataSource = strucmotifConfig.getDataSource();
        this.dataPath = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.RENUMBERED + StrucmotifConfig.DATA_EXT);
        this.indexPath = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.RENUMBERED + StrucmotifConfig.INDEX_EXT);
        this.partialDataPath = dataPath.resolveSibling(dataPath.getFileName() + StrucmotifConfig.PARTIAL_EXT);
        this.partialIndexPath = indexPath.resolveSibling(indexPath.getFileName() + StrucmotifConfig.PARTIAL_EXT);
        this.temporaryDataPath = dataPath.resolveSibling(dataPath.getFileName() + StrucmotifConfig.TMP_EXT);
        this.temporaryIndexPath = indexPath.resolveSibling(indexPath.getFileName() + StrucmotifConfig.TMP_EXT);
        this.extension = ".bcif.gz";

        logger.info("BinaryCIF data source is {} - CIF fetch URL: {} - precision: {}",
                strucmotifConfig.getDataSource(),
                strucmotifConfig.getCifFetchUrl(),
                strucmotifConfig.getRenumberedCoordinatePrecision());

        this.caching = false;
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

    private String getRenumberedFilename(String structureIdentifier) {
        return structureIdentifier + extension;
    }

    private InputStream getRenumberedInputStream(String structureIdentifier) {
        try {
            return toInputStream(fileBundle.readFile(getRenumberedFilename(structureIdentifier)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream toInputStream(ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        byte[] out = new byte[byteBuffer.remaining()];
        byteBuffer.get(out);
        return new ByteArrayInputStream(out);
    }

    @PostConstruct
    public void setUp() throws IOException {
        try {
            // during update, it's OK to start with non-existent files
            if (Files.notExists(dataPath)) {
                Files.createFile(dataPath);
                logger.debug("Created renumbered structure data file '{}'", dataPath);
            }
            if (Files.notExists(indexPath)) {
                Files.createFile(indexPath);
                logger.debug("Created renumbered structure index file '{}'", indexPath);
            }
        } catch (NoSuchFileException e) {
            logger.error("Could not create renumbered structure file bundle (data path: '{}', index path: '{}') - make sure that '{}' exists and is accessible",
                    dataPath,
                    indexPath,
                    dataPath.getParent());
            throw e;
        }

        initializeFileBundle();
        Files.deleteIfExists(partialDataPath);
        Files.deleteIfExists(partialIndexPath);
        initializePartialFileBundle();
        Files.deleteIfExists(temporaryDataPath);
        Files.deleteIfExists(temporaryIndexPath);

        initializeRenumberedStructureCache();
    }

    @PreDestroy
    public void tearDown() throws IOException {
        fileBundle.close();
        partialFileBundle.close();
        Files.deleteIfExists(partialDataPath);
        Files.deleteIfExists(partialIndexPath);
        Files.deleteIfExists(temporaryDataPath);
        Files.deleteIfExists(temporaryIndexPath);
    }

    private void initializeFileBundle() throws IOException {
        logger.debug("Opening renumbered file bundle ({}, {})", dataPath, indexPath);
        this.fileBundle = FileBundleIO.openBundle(dataPath, indexPath).inReadOnlyMode();
    }

    private void initializePartialFileBundle() throws IOException {
        logger.debug("Creating partial renumbered file bundle ({}, {})", partialDataPath, partialIndexPath);
        this.partialFileBundle = FileBundleIO.openBundle(partialDataPath, partialIndexPath).inReadWriteMode();
    }

    private WritableFileBundle initializeTemporaryFileBundle() throws IOException {
        logger.debug("Creating temporary renumbered file bundle ({}, {})", temporaryDataPath, temporaryIndexPath);
        return FileBundleIO.openBundle(temporaryDataPath, temporaryIndexPath).inReadWriteMode();
    }

    @Override
    public void initializeRenumberedStructureCache() {
        InMemoryStrategy strategy = strucmotifConfig.getInMemoryStrategy();
        if (strategy == InMemoryStrategy.OFF) {
            logger.info("Structure data will be read from file-system");
            return;
        }

        // TODO add off-heap strategy
        if (strategy == InMemoryStrategy.HEAP) {
            logger.info("Structure data will be kept in memory - start loading...");

            this.caching = true;
            List<String> filenames = fileBundle.filenames().collect(Collectors.toList());
            long start = System.nanoTime();
            this.structureCache = new HashMap<>();

            int loadingChunkSize = strucmotifConfig.getLoadingChunkSize();
            Partition<String> partitions = new Partition<>(filenames, loadingChunkSize);
            logger.info("Formed {} partitions of {} structures",
                    partitions.size(),
                    loadingChunkSize);

            for (int i = 0; i < partitions.size(); i++) {
                String partitionContext = (i + 1) + " / " + partitions.size();

                List<String> partition = partitions.get(i);
                logger.info("[{}] Start loading partition", partitionContext);

                // this will run on strucmotif-instances only: let's ignore thread-parameter
                Map<String, Structure> buffer = partition.parallelStream()
                        .map(this::loadRenumberedStructurePair)
                        .collect(Collectors.toConcurrentMap(Pair::getFirst, Pair::getSecond));

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

    private Pair<String, Structure> loadRenumberedStructurePair(String filename) {
        String pdbId = filename.split("\\.")[0];
        return new Pair<>(pdbId, readFromInputStream(getRenumberedInputStream(pdbId)));
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

        // during the update, this file might only be known in the partial update file
        String filename = getRenumberedFilename(structureIdentifier);
        if (partialFileBundle.containsFile(filename)) {
            try {
                return readFromInputStream(toInputStream(partialFileBundle.readFile(filename)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
                if (fileBundle.containsFile(getRenumberedFilename(structureIdentifier))) {
                    return readFromInputStream(getRenumberedInputStream(structureIdentifier));
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
        byte[] bytes = renumberedStructureWriter.write(mmCifFile);
        if (bytes == null) {
            logger.warn("[{}] No valid polymer chain(s) - Not writing empty file", structureIdentifier);
            return;
        }

        try {
            partialFileBundle.writeFile(getRenumberedFilename(structureIdentifier), ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteRenumbered(Collection<String> structureIdentifiers) {
        String[] filenamesToDrop = structureIdentifiers.stream()
                .map(this::getRenumberedFilename)
                .distinct()
                .toArray(String[]::new);

        try {
            fileBundle.close();
            FileBundleIO.unlinkFiles(indexPath, filenamesToDrop);
            FileBundleIO.compactBundle(dataPath, indexPath);
            initializeFileBundle();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void commit() {
        try {
            logger.debug("Adding original data");
            Files.copy(dataPath, temporaryDataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(indexPath, temporaryIndexPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Adding partial data");
            partialFileBundle.close();
            FileBundleIO.mergeBundles(temporaryDataPath, temporaryIndexPath, partialDataPath, partialIndexPath);
            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();

            logger.debug("Deleting partial file bundle and swap temporary files with real ones");
            fileBundle.close();
            temporaryFileBundle.close();
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(partialDataPath);
            Files.deleteIfExists(partialIndexPath);
            initializeFileBundle();
            initializePartialFileBundle();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<String> reportKnownFiles() {
        return fileBundle.filenames().collect(Collectors.toSet());
    }
}
