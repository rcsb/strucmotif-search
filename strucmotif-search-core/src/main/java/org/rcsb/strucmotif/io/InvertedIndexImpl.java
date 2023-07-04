package org.rcsb.strucmotif.io;

import org.rcsb.ffindex.AppendableFileBundle;
import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.ffindex.WritableFileBundle;
import org.rcsb.strucmotif.config.InvertedIndexBackend;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.io.codec.BucketCodec;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The implementation of the inverted index.
 */
@Service
public class InvertedIndexImpl implements InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndexImpl.class);
    private static final Map<String, ResidueType> OLC_LOOKUP = Stream.of(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getInternalCode, Function.identity()));
    private final String extension;
    private final BucketCodec bucketCodec;
    private final ThreadPool threadPool;
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

    /**
     * Construct an inverted index instance.
     * @param threadPool shared pool for parallel operations
     * @param strucmotifConfig the config
     */
    public InvertedIndexImpl(ThreadPool threadPool, StrucmotifConfig strucmotifConfig) {
        InvertedIndexBackend invertedIndexBackend = strucmotifConfig.getInvertedIndexBackend();
        this.bucketCodec = invertedIndexBackend.getBucketCodec();
        this.extension = invertedIndexBackend.getExtension();
        logger.info("Extension of inverted index files: {}", extension);
        this.threadPool = threadPool;
        this.dataPath = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.INDEX + StrucmotifConfig.DATA_EXT);
        this.indexPath = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.INDEX + StrucmotifConfig.INDEX_EXT);
        this.partialDataPath = dataPath.resolveSibling(dataPath.getFileName() + StrucmotifConfig.PARTIAL_EXT);
        this.partialIndexPath = indexPath.resolveSibling(indexPath.getFileName() + StrucmotifConfig.PARTIAL_EXT);
        this.temporaryDataPath = dataPath.resolveSibling(dataPath.getFileName() + StrucmotifConfig.TMP_EXT);
        this.temporaryIndexPath = indexPath.resolveSibling(indexPath.getFileName() + StrucmotifConfig.TMP_EXT);
    }

    @PostConstruct
    public void setUp() throws IOException {
        try {
            // during update, it's OK to start with non-existent files
            if (Files.notExists(dataPath)) {
                Files.createFile(dataPath);
                logger.debug("Created inverted index data file '{}'", dataPath);
            }
            if (Files.notExists(indexPath)) {
                Files.createFile(indexPath);
                logger.debug("Created inverted index index file '{}'", indexPath);
            }
        } catch (NoSuchFileException e) {
            logger.error("Could not create inverted index file bundle (data path: '{}', index path: '{}') - make sure that '{}' exists and is accessible",
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
        logger.debug("Opening index file bundle ({}, {})", dataPath, indexPath);
        this.fileBundle = FileBundleIO.openBundle(dataPath, indexPath).inReadOnlyMode();
    }

    private void initializePartialFileBundle() throws IOException {
        logger.debug("Creating partial index file bundle ({}, {})", partialDataPath, partialIndexPath);
        this.partialFileBundle = FileBundleIO.openBundle(partialDataPath, partialIndexPath).inReadWriteMode();
    }

    private WritableFileBundle initializeTemporaryFileBundle() throws IOException {
        logger.debug("Creating temporary index file bundle ({}, {})", temporaryDataPath, temporaryIndexPath);
        return FileBundleIO.openBundle(temporaryDataPath, temporaryIndexPath).inWriteOnlyMode();
    }

    @Override
    public void insert(int residuePairDescriptor, InvertedIndexBucket bucket, int batchId) {
        if (bucket.getResiduePairCount() == 0) {
            throw new IllegalStateException("won't write empty bucket for " + residuePairDescriptor);
        }

        // write a temporary file (appended by the batchId)
        String filename = getFilename(residuePairDescriptor, batchId);
        try {
            ByteBuffer byteBuffer = bucketCodec.encode(bucket);
            partialFileBundle.writeFile(filename, byteBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException("can't write " + filename, e);
        }
    }

    @Override
    public void commit() {
        logger.info("Committing temporary files to index");
        try {
            // this captures the original data
            Map<String, Set<String>> toMerge = indexFilenames()
                    .collect(Collectors.toMap(Function.identity(), v -> new HashSet<>()));
            // this captures all additional data
            partialFilenames()
                    .forEach(partialFilename -> {
                        String persistentFilename = partialFilename.substring(0, partialFilename.lastIndexOf("."));
                        Set<String> bin = toMerge.computeIfAbsent(persistentFilename, e -> new HashSet<>());
                        bin.add(partialFilename);
                    });
            logger.info("Merging {} bins", toMerge.size());

            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();
            threadPool.submit(() -> {
                AtomicInteger counter = new AtomicInteger();
                toMerge.entrySet()
                        .parallelStream()
                        .peek(p -> progress(counter, 50000, "{} / " + toMerge.size() + " files merged"))
                        .forEach(entry -> {
                            String destination = entry.getKey();
                            Set<String> sources = entry.getValue();

                            try {
                                Map<Integer, Collection<ResiduePairIdentifier>> merged = new HashMap<>();
                                // populate with existing data
                                if (fileBundle.containsFile(destination)) {
                                    InvertedIndexBucket existing = bucketCodec.decode(fileBundle.readFile(destination));
                                    addAll(merged, existing, null);
                                }

                                // merge all new, partial data
                                for (String s : sources) {
                                    InvertedIndexBucket additions = bucketCodec.decode(partialFileBundle.readFile(s));
                                    addAll(merged, additions, null);
                                }

                                ByteBuffer output = bucketCodec.encode(new ResiduePairIdentifierBucket(merged));
                                temporaryFileBundle.writeFile(destination, output);
                            } catch (IOException e) {
                                throw new UncheckedIOException("can't merge " + destination, e);
                            }
                        });
                return null;
            }).get();

            // delete partial file bundle and swap temporary files with real ones
            fileBundle.close();
            partialFileBundle.close();
            temporaryFileBundle.close();
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(partialDataPath);
            Files.deleteIfExists(partialIndexPath);
            initializeFileBundle();
            initializePartialFileBundle();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel operation failed - Thread raised exception", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel operation failed - Thread was interrupted", e);
        }
    }

    @Override
    public InvertedIndexBucket select(ResiduePairDescriptor residuePairDescriptor) {
        String filename = getFilename(residuePairDescriptor);
        if (!fileBundle.containsFile(filename)) {
            return InvertedIndexBucket.EMPTY_BUCKET;
        }

        try {
            return bucketCodec.decode(getByteBuffer(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("can't read " + filename, e);
        }
    }

    private ByteBuffer getByteBuffer(String filename) throws IOException {
        return fileBundle.readFile(filename);
    }

    private String getFilename(ResiduePairDescriptor residuePairDescriptor) {
        String bin = residuePairDescriptor.toString();
        String uberbin = bin.substring(0, 2);
        return uberbin + "/" + bin + extension;
    }

    private String getFilename(int residuePairDescriptor, int batchId) {
        return getFilename(residuePairDescriptor) + "." + batchId;
    }

    @Override
    public void delete(Collection<Integer> removals) {
        try {
            logger.info("Removing {} structures from inverted index", removals.size());
            int fileCount = fileBundle.fileCount();

            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();
            AtomicInteger counter = new AtomicInteger();
            // walk whole lookup
            threadPool.submit(() -> {
                indexFilenames()
                        .parallel()
                        .peek(path -> progress(counter, 10000, "{} / " + fileCount + " bins of inverted index processed"))
                        .forEach(filename -> {
                            try {
                                ResiduePairDescriptor residuePairDescriptor = createResiduePairDescriptor(filename);
                                ByteBuffer byteBuffer = delete(residuePairDescriptor, removals);

                                // result may be empty, don't write anything in that case
                                if (byteBuffer == null) {
                                    return;
                                }

                                temporaryFileBundle.writeFile(filename, byteBuffer);
                            } catch (IOException e) {
                                throw new UncheckedIOException("can't process " + filename, e);
                            }
                        });
                return null;
            }).get();

            // swap new and old files
            fileBundle.close();
            temporaryFileBundle.close();
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            initializeFileBundle();
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel operation failed - Thread raised exception", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel operation failed - Thread was interrupted", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void progress(AtomicInteger counter, int interval, String message) {
        int i = counter.incrementAndGet();
        if (i % interval == 0) {
            logger.info(message, i);
        }
    }

    private ResiduePairDescriptor createResiduePairDescriptor(String filename) {
        String[] filenameSplit = filename.split("/");
        String name = filenameSplit[filenameSplit.length - 1];

        String[] split = name.split("\\.")[0].split("-");
        ResidueType residueType1 = OLC_LOOKUP.getOrDefault(split[0].substring(0, 1), null);
        ResidueType residueType2 = OLC_LOOKUP.getOrDefault(split[0].substring(1, 2), null);
        DistanceType d1 = DistanceType.ofIntRepresentation(Integer.parseInt(split[1]));
        DistanceType d2 = DistanceType.ofIntRepresentation(Integer.parseInt(split[2]));
        AngleType a = AngleType.ofIntRepresentation(Integer.parseInt(split[3]));
        return ResiduePairDescriptor.encodeDescriptor(residueType1, residueType2, d1, d2, a);
    }

    /**
     * Remove structures from the bucket, based on their structure identifiers.
     * @param residuePairDescriptor the descriptor to process
     * @param removals what to remove
     * @return a {@link ByteBuffer} with a subset of the original bucket or null if this operation resulted in an empty
     * bucket
     * @throws IOException when encoding fails
     */
    private ByteBuffer delete(ResiduePairDescriptor residuePairDescriptor, Collection<Integer> removals) throws IOException {
        ByteBuffer byteBuffer = getByteBuffer(getFilename(residuePairDescriptor));
        InvertedIndexBucket bucket = bucketCodec.decode(byteBuffer);
        Set<Integer> structureIndices = bucket.getStructureIndices();

        // if no entry would be removed: don't bother and return
        if (removals.stream().noneMatch(structureIndices::contains)) {
            byteBuffer.rewind();
            return byteBuffer;
        }

        // remove all occurrences of structure identifiers
        ResiduePairIdentifierBucket filteredBucket = removeByKey(bucket, removals);
        if (filteredBucket == null) {
            return null;
        }

        // serialize message
        return bucketCodec.encode(filteredBucket);
    }

    @Override
    public Set<ResiduePairDescriptor> reportKnownDescriptors() {
        return indexFilenames()
                .parallel()
                .map(this::createResiduePairDescriptor)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> reportKnownKeys() {
        try {
            logger.info("Collecting all known keys in bundle ({}, {})", dataPath, indexPath);
            AtomicInteger counter = new AtomicInteger();
            return threadPool.submit(() -> indexFilenames()
                    .parallel()
                    .peek(p -> progress(counter, 10000, "{} bins scanned"))
                    .map(this::createResiduePairDescriptor)
                    .map(this::select)
                    .map(Bucket::getStructureIndices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet())).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel operation failed - Thread raised exception", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel operation failed - Thread was interrupted", e);
        }
    }

    private Stream<String> indexFilenames() {
        return fileBundle.filenames();
    }

    private Stream<String> partialFilenames() {
        return partialFileBundle.filenames();
    }

    private void addAll(Map<Integer, Collection<ResiduePairIdentifier>> map, Bucket bucket, Collection<Integer> ignore) {
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            int key = bucket.getStructureIndex();
            if (ignore != null && ignore.contains(key)) {
                continue;
            }
            if (map.containsKey(key)) {
                throw new IllegalStateException("Duplicate key: " + key);
            }
            Collection<ResiduePairIdentifier> identifiers = map.computeIfAbsent(key, e -> new ArrayList<>());

            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();

                ResiduePairIdentifier residuePairIdentifier = bucket.getResiduePairIdentifier();
                identifiers.add(residuePairIdentifier);
            }
        }
    }

    /**
     * Remove a collection of values from a bucket. Doesn't manipulate the original bucket.
     * @param bucket the source
     * @param removals what to remove
     * @return a new bucket that doesn't contain any of the removals
     */
    private ResiduePairIdentifierBucket removeByKey(InvertedIndexBucket bucket, Collection<Integer> removals) {
        Map<Integer, Collection<ResiduePairIdentifier>> map = new HashMap<>();
        addAll(map, bucket, removals);

        // report empty maps as null
        if (map.isEmpty()) {
            return null;
        }

        return new ResiduePairIdentifierBucket(map);
    }
}
