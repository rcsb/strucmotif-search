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
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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
    private static final int BUFFER_SIZE = 65536;
    private static final byte MAGIC = (byte) (1 << 7);
    private final String extension;
    private final BucketCodec bucketCodec;
    private final ThreadPool threadPool;
    // 'production' data that can be queried
    private final Path rootPath;
    private final Path dataPath;
    private final Path indexPath;
    private ReadableFileBundle fileBundle;
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
        this.rootPath = Paths.get(strucmotifConfig.getRootPath());
        this.dataPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.DATA_EXT);
        this.indexPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.INDEX_EXT);
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
            Set<Integer> existingDescriptors = reportKnownDescriptors();

            // this captures all additional data
            List<Path> partials = partialFilenames().collect(Collectors.toList());
            logger.info("Merging partial data from {}", partials);
            // key: structure index, value: source/partial file
            Map<Integer, RandomAccessFile> partialFileMapping = new HashMap<>();
            // outer key: residue pair descriptor, inner key: structure index, value: begin of data in corresponding file
            Map<Integer, Map<Integer, Long>> startOffsets = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> byteCounts = new HashMap<>();
            byte[] readBuffer = new byte[BUFFER_SIZE];
            for (Path p : partials) {
                RandomAccessFile file = new RandomAccessFile(p.toFile(), "r");
                AtomicInteger lastDescriptor = new AtomicInteger(Integer.MAX_VALUE);
                AtomicInteger lastStructureIndex = new AtomicInteger(Integer.MAX_VALUE);
                long offset = 0L;
                int read;
                while ((read = file.read(readBuffer)) > 0) {
                    for (int i = 0; i < read - 7; i = i + 8, offset = offset + 8) {
                        if ((readBuffer[i] & MAGIC) == 0) {
                            continue;
                        }
                        if (lastDescriptor.get() != Integer.MAX_VALUE) {
                            int descriptor = lastDescriptor.get();
                            Map<Integer, Integer> lengthMap = byteCounts.computeIfAbsent(descriptor, e -> new HashMap<>());
                            int structureIndex = lastStructureIndex.get();
                            lengthMap.put(structureIndex, (int) (offset - startOffsets.get(descriptor).get(structureIndex)));
                        }
                        int structureIndex = (readBuffer[i] & ~MAGIC & 0xFF) << 24 | (readBuffer[i + 1] & 0xFF) << 16 | (readBuffer[i + 2] & 0xFF) << 8 | (readBuffer[i + 3] & 0xFF);
                        partialFileMapping.put(structureIndex, file);
                        int descriptor = (readBuffer[i + 4] & 0xFF) << 24 | (readBuffer[i + 5] & 0xFF) << 16 | (readBuffer[i + 6] & 0xFF) << 8 | (readBuffer[i + 7] & 0xFF);
                        Map<Integer, Long> startMap = startOffsets.computeIfAbsent(descriptor, e -> new HashMap<>());
                        startMap.put(structureIndex, offset + 8); // identifier data will start in next block
                        lastDescriptor.set(descriptor);
                        lastStructureIndex.set(structureIndex);
                    }
                }

                // capture last datum
                int descriptor = lastDescriptor.get();
                // files might have been created but no content was written
                if (descriptor == Integer.MAX_VALUE) {
                    continue;
                }
                Map<Integer, Integer> lengthMap = byteCounts.computeIfAbsent(descriptor, e -> new HashMap<>());
                int structureIndex = lastStructureIndex.get();
                lengthMap.put(structureIndex, (int) (offset - startOffsets.get(descriptor).get(structureIndex)));
            }

            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();
            Set<Integer> existingOnly = existingDescriptors.stream().filter(i -> !startOffsets.containsKey(i)).collect(Collectors.toSet());
            if (!existingOnly.isEmpty()) {
                logger.info("Transferring existing data for {} descriptors", existingOnly.size());
                ByteBuffer tmp;
                for (int descriptor : existingOnly) {
                    String filename = descriptor + extension;
                    // TODO might exceed limit on memory-mapped regions, possible to harden this? move upstream to FileBundleIO?
                    tmp = fileBundle.readFile(filename);
                    temporaryFileBundle.writeFile(filename, tmp);
                }
            }

            logger.info("Merging data on {} descriptors from {} structures", startOffsets.size(), partialFileMapping.size());
            AtomicInteger counter = new AtomicInteger();
            startOffsets.entrySet()
                    .parallelStream()
                    .peek(p -> progress(counter, 1000, "[{} / " + startOffsets.size() + "] compacting descriptor data"))
                    .forEach(entry -> {
                        int descriptor = entry.getKey();
                        Map<Integer, Long> perStructureStart = entry.getValue();
                        Map<Integer, Integer> perStructureByteCount = byteCounts.get(descriptor);
                        int structureCount = perStructureStart.size();
                        int identifierCount = perStructureStart.keySet().stream().mapToInt(si -> perStructureByteCount.get(si) / 4).sum();

                        try {
                            int[] structureIndices;
                            int[] positionOffsets;
                            int[] identifierData;
                            int outerPos = 0;
                            int innerPos = 0;

                            // check if there's data in production files, if so concat that to the start of the arrays
                            if (fileBundle.containsFile(descriptor + extension)) {
                                ByteBuffer byteBuffer = fileBundle.readFile(descriptor + extension);
                                InvertedIndexBucket existingBucket = bucketCodec.decode(byteBuffer);

                                int existingStructureCount = existingBucket.getStructureIndexArray().length;
                                int existingIdentifierCount = existingBucket.getIdentifierDataArray().length;
                                structureIndices = new int[structureCount + existingStructureCount];
                                positionOffsets = new int[structureCount + existingStructureCount];
                                identifierData = new int[identifierCount + existingIdentifierCount];

                                System.arraycopy(existingBucket.getStructureIndexArray(), 0, structureIndices, 0, existingStructureCount);
                                System.arraycopy(existingBucket.getPositionOffsetArray(), 0, positionOffsets, 0, existingStructureCount);
                                System.arraycopy(existingBucket.getIdentifierDataArray(), 0, identifierData, 0, existingIdentifierCount);

                                // advance positions accordingly
                                outerPos += existingStructureCount;
                                innerPos += existingIdentifierCount;
                            } else {
                                structureIndices = new int[structureCount];
                                positionOffsets = new int[structureCount];
                                identifierData = new int[identifierCount];
                            }

                            for (Map.Entry<Integer, Long> datum : perStructureStart.entrySet()) {
                                int structureIndex = datum.getKey();
                                structureIndices[outerPos] = structureIndex;
                                positionOffsets[outerPos] = innerPos;

                                RandomAccessFile sourceFile = partialFileMapping.get(structureIndex);
                                int n = perStructureByteCount.get(structureIndex);
                                int perStructureIdentifierCount = n / 4;
                                ByteBuffer buffer = ByteBuffer.allocate(n);
                                sourceFile.getChannel().read(buffer, perStructureStart.get(structureIndex));
                                buffer.rewind();
                                for (int i = 0; i < perStructureIdentifierCount; i++) {
                                    identifierData[innerPos + i] = buffer.getInt();
                                }

                                outerPos++;
                                innerPos += perStructureIdentifierCount;
                            }

                            InvertedIndexBucket bucket = new InvertedIndexBucket(structureIndices, positionOffsets, identifierData);
                            ByteBuffer out = bucketCodec.encode(bucket);
                            temporaryFileBundle.writeFile(descriptor + extension, out);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });

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

    private Stream<Path> partialFilenames() throws IOException {
        logger.info("Collecting partial files at {}", rootPath);
        return Files.list(rootPath)
                .filter(p -> p.getFileName().toString().startsWith(StrucmotifConfig.INDEX) && p.getFileName().toString().endsWith(StrucmotifConfig.TMP_EXT));
    }

    private void deletePartialFiles() throws IOException {
        partialFilenames().map(Path::toFile).forEach(File::delete);
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
        int[] identifierData = new int[originalIdentifierData.length]; // assume worst-case for now

        int outerPos = 0;
        int identifierPos = 0;
        int removeIndex = 0;
        for (int i = 0; i < originalLength; i++) {
            if (removeIndex < indicesToRemove.length && i == indicesToRemove[removeIndex]) {
                removeIndex++;
                continue;
            }

            structureIndices[outerPos] = originalStructureIndices[i];
            positionOffsets[outerPos] = identifierPos;
            // copy partial range to output array
            int identifierCopyCount = (i + 1 == originalPositionOffsets.length ? originalIdentifierData.length : originalPositionOffsets[i + 1]) - originalPositionOffsets[i];
            System.arraycopy(originalIdentifierData, originalPositionOffsets[i], identifierData, identifierPos, identifierCopyCount);

            outerPos++;
            identifierPos += identifierCopyCount;
        }
        identifierData = Arrays.copyOf(identifierData, identifierPos);

        return new InvertedIndexBucket(structureIndices, positionOffsets, identifierData);
    }
}
