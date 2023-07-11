package org.rcsb.strucmotif.io;

import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.ffindex.WritableFileBundle;
import org.rcsb.strucmotif.config.InvertedIndexBackend;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.io.codec.BucketCodec;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The implementation of the inverted index.
 */
@Service
public class DefaultInvertedIndex implements InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(DefaultInvertedIndex.class);
    private final String extension;
    private final BucketCodec bucketCodec;
    private final ThreadPool threadPool;
    // 'production' data that can be queried
    private final Path dataPath;
    private final Path indexPath;
    private ReadableFileBundle fileBundle;
    // dump of partial files from update
    private final Path partialPath;
    // paths for 'temporary' bundle written when 'production' data is getting modified
    private final Path temporaryDataPath;
    private final Path temporaryIndexPath;

    /**
     * Construct an inverted index instance.
     * @param threadPool shared pool for parallel operations
     * @param strucmotifConfig the config
     */
    public DefaultInvertedIndex(ThreadPool threadPool, StrucmotifConfig strucmotifConfig) {
        InvertedIndexBackend invertedIndexBackend = strucmotifConfig.getInvertedIndexBackend();
        this.bucketCodec = invertedIndexBackend.getBucketCodec();
        this.extension = invertedIndexBackend.getExtension();
        logger.info("Extension of inverted index files: {}", extension);
        this.threadPool = threadPool;
        Path rootPath = Paths.get(strucmotifConfig.getRootPath());
        this.dataPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.DATA_EXT);
        this.indexPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.INDEX_EXT);
        this.partialPath = rootPath.resolve(StrucmotifConfig.PARTIAL);
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
            if (Files.notExists(partialPath)) {
                Files.createDirectories(partialPath);
                logger.debug("Created inverted index tmp directory '{}'", partialPath);
            }
        } catch (NoSuchFileException e) {
            logger.error("Could not create inverted index file bundle (data path: '{}', index path: '{}') - make sure that '{}' exists and is accessible",
                    dataPath,
                    indexPath,
                    dataPath.getParent());
            throw e;
        }

        initializeFileBundle();
        deletePartialFiles();
        Files.deleteIfExists(temporaryDataPath);
        Files.deleteIfExists(temporaryIndexPath);
    }

    @PreDestroy
    public void tearDown() throws IOException {
        fileBundle.close();
        Files.deleteIfExists(temporaryDataPath);
        Files.deleteIfExists(temporaryIndexPath);
    }

    private void initializeFileBundle() throws IOException {
        logger.debug("Opening index file bundle ({}, {})", dataPath, indexPath);
        this.fileBundle = FileBundleIO.openBundle(dataPath, indexPath).inReadOnlyMode();
    }

    private WritableFileBundle initializeTemporaryFileBundle() throws IOException {
        logger.debug("Creating temporary index file bundle ({}, {})", temporaryDataPath, temporaryIndexPath);
        return FileBundleIO.openBundle(temporaryDataPath, temporaryIndexPath).inWriteOnlyMode();
    }

    @Override
    public void commit() {
        logger.info("Committing temporary files to index");
        try {
            // this captures all additional data
            Map<String, Set<String>> toMerge = new HashMap<>();
            partialFilenames()
                    .forEach(partialFilename -> {
                        String persistentFilename = partialFilename.split("\\.")[0];
                        Set<String> bin = toMerge.computeIfAbsent(persistentFilename, e -> new HashSet<>());
                        bin.add(partialFilename);
                    });
            logger.info("Merging {} bins", toMerge.size());

            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();
            AtomicInteger counter = new AtomicInteger();
            toMerge.entrySet()
                    .stream()
                    .peek(p -> progress(counter, 1, "[{} / " + toMerge.size() + "] merging super-bin"))
                    .forEach(b -> mergeSuperBin(b, temporaryFileBundle));

            // delete partial file bundle and swap temporary files with real ones
            fileBundle.close();
            temporaryFileBundle.close();
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            deletePartialFiles();
            initializeFileBundle();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void mergeSuperBin(Map.Entry<String, Set<String>> entry, WritableFileBundle temporaryFileBundle) {
        Set<String> sources = entry.getValue();
        Set<Integer> descriptors = sources.stream().flatMapToInt(this::extractDescriptors).boxed().collect(Collectors.toSet());

        try {
            threadPool.submit(() -> {
                descriptors.parallelStream().forEach(descriptor -> {
                    try {
                        String destination = descriptor + extension;
                        List<InvertedIndexBucket> buckets = new ArrayList<>();
                        // populate with existing data
                        if (fileBundle.containsFile(destination)) {
                            buckets.add(bucketCodec.decode(fileBundle.readFile(destination)));
                        }

                        // merge all new, partial data
                        for (String source : sources) {
                            AtomicInteger structureIndex = new AtomicInteger(-1);
                            AtomicBoolean indexAdded = new AtomicBoolean(false);
                            List<Integer> structureIndices = new ArrayList<>();
                            List<Integer> positionOffsets = new ArrayList<>();
                            List<Integer> identifierData = new ArrayList<>();
                            Files.lines(partialPath.resolve(source)).forEach(l -> {
                                String[] split = l.split(" ");
                                if (split.length == 1) {
                                    structureIndex.set(Integer.parseInt(l));
                                    return;
                                }
                                if (descriptor != Integer.parseInt(split[0])) {
                                    return;
                                }

                                if (!indexAdded.get()) {
                                    structureIndices.add(structureIndex.get());
                                    indexAdded.set(true);
                                    positionOffsets.add(identifierData.size());
                                }
                                identifierData.add(Integer.parseInt(split[1]));
                                identifierData.add(Integer.parseInt(split[2]));
                            });

                            // don't track empty buckets
                            if (structureIndices.isEmpty()) {
                                continue;
                            }

                            buckets.add(new InvertedIndexBucket(structureIndices.stream().mapToInt(Integer::intValue).toArray(),
                                    positionOffsets.stream().mapToInt(Integer::intValue).toArray(),
                                    identifierData.stream().mapToInt(Integer::intValue).toArray()));
                        }

                        int structureCount = buckets.stream().mapToInt(InvertedIndexBucket::getStructureCount).sum();
                        int identifierCount = buckets.stream().mapToInt(InvertedIndexBucket::getResiduePairCount).sum() * 2;
                        int outerPosition = 0;
                        int innerPosition = 0;
                        int[] mergedStructureIndices = new int[structureCount];
                        int[] mergedPositionOffsets = new int[structureCount];
                        int[] mergedIdentifierData = new int[identifierCount];
                        for (InvertedIndexBucket bucket : buckets) {
                            System.arraycopy(bucket.getStructureIndexArray(), 0, mergedStructureIndices, outerPosition, bucket.getStructureIndexArray().length);

                            int[] positionOffsetArray = bucket.getPositionOffsetArray();
                            for (int i = 0; i < positionOffsetArray.length; i++) {
                                // position offsets must be shifted according to the data already present
                                mergedPositionOffsets[outerPosition + i] = positionOffsetArray[i] + innerPosition;
                            }

                            System.arraycopy(bucket.getIdentifierDataArray(), 0, mergedIdentifierData, innerPosition, bucket.getIdentifierDataArray().length);
                            outerPosition += bucket.getStructureIndexArray().length;
                            innerPosition += bucket.getIdentifierDataArray().length;
                        }

                        ByteBuffer output = bucketCodec.encode(new InvertedIndexBucket(mergedStructureIndices, mergedPositionOffsets, mergedIdentifierData));
                        temporaryFileBundle.writeFile(destination, output);
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to merge " + entry.getKey(), e);
                    }
                });
                return null;
            }).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Parallel operation failed - Thread raised exception", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel operation failed - Thread was interrupted", e);
        }

        try {
            for (String source : sources) {
                Files.delete(partialPath.resolve(source));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private IntStream extractDescriptors(String source) {
        try {
            return Files.lines(partialPath.resolve(source))
                    .map(l -> l.split(" "))
                    .filter(s -> s.length == 3)
                    .mapToInt(s -> Integer.parseInt(s[0]));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InvertedIndexBucket select(int residuePairDescriptor) {
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

    private String getFilename(int residuePairDescriptor) {
        return residuePairDescriptor + extension;
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
                                int residuePairDescriptor = createResiduePairDescriptor(filename);
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

    private int createResiduePairDescriptor(String filename) {
        return Integer.parseInt(filename.split("\\.")[0]);
    }

    /**
     * Remove structures from the bucket, based on their structure identifiers.
     * @param residuePairDescriptor the descriptor to process
     * @param removals what to remove
     * @return a {@link ByteBuffer} with a subset of the original bucket or null if this operation resulted in an empty
     * bucket
     * @throws IOException when encoding fails
     */
    private ByteBuffer delete(int residuePairDescriptor, Collection<Integer> removals) throws IOException {
        ByteBuffer byteBuffer = getByteBuffer(getFilename(residuePairDescriptor));
        InvertedIndexBucket bucket = bucketCodec.decode(byteBuffer);
        Set<Integer> structureIndices = bucket.getStructureIndices();

        // if no entry would be removed: don't bother and return
        if (removals.stream().noneMatch(structureIndices::contains)) {
            byteBuffer.rewind();
            return byteBuffer;
        }

        // remove all occurrences of structure identifiers
        InvertedIndexBucket filteredBucket = removeByKey(bucket, removals);
        if (filteredBucket == null) {
            return null;
        }

        // serialize message
        return bucketCodec.encode(filteredBucket);
    }

    @Override
    public Set<Integer> reportKnownDescriptors() {
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
                    .map(InvertedIndexBucket::getStructureIndices)
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

    private Stream<String> partialFilenames() throws IOException {
        logger.info("Collecting files at {}", partialPath);
        return Files.list(partialPath)
                .map(Path::getFileName)
                .map(Path::toString);
    }

    private void deletePartialFiles() throws IOException {
        Files.list(partialPath).map(Path::toFile).forEach(File::delete);
    }

    /**
     * Remove a collection of values from a bucket. Doesn't manipulate the original bucket.
     * @param bucket the source
     * @param removals what to remove
     * @return a new bucket that doesn't contain any of the removals
     */
    private InvertedIndexBucket removeByKey(InvertedIndexBucket bucket, Collection<Integer> removals) {
        int[] originalStructureIndices = bucket.getStructureIndexArray();
        int[] originalPositionOffsets = bucket.getPositionOffsetArray();
        int[] originalIdentifierData = bucket.getIdentifierDataArray();
        int originalLength = originalStructureIndices.length;
        int[] indicesToRemove = IntStream.range(0, originalLength).filter(i -> removals.contains(originalStructureIndices[i])).toArray();

        // report empty return as null
        if (originalLength == indicesToRemove.length) {
            return null;
        }

        int updatedLength = originalLength - indicesToRemove.length;
        int[] structureIndices = new int[updatedLength];
        int[] positionOffsets = new int[updatedLength];
        int acceptedIdentifierCount = 0;
        int[] identifierData = new int[originalIdentifierData.length]; // assume worst-case for now

        int outerPos = 0;
        int identifierPos = 0;
        int removeIndex = 0;
        for (int i = 0; i < originalLength; i++) {
            if (i < indicesToRemove[removeIndex]) {
                continue;
            }

            structureIndices[outerPos] = originalStructureIndices[i];
            positionOffsets[outerPos] = originalPositionOffsets[i];
            // copy partial range to output array
            int identifierCopyCount = i + 1 == originalIdentifierData.length ? originalIdentifierData.length : originalIdentifierData[i + 1] - originalIdentifierData[i];
            System.arraycopy(originalIdentifierData, originalPositionOffsets[i], identifierData, identifierPos, identifierCopyCount);

            outerPos++;
            identifierPos += identifierCopyCount;
            removeIndex++;
        }
        System.arraycopy(identifierData, 0, identifierData, 0, acceptedIdentifierCount);

        return new InvertedIndexBucket(structureIndices, positionOffsets, identifierData);
    }
}
