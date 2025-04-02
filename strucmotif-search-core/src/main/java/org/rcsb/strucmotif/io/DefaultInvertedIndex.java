package org.rcsb.strucmotif.io;

import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.ffindex.WritableFileBundle;
import org.rcsb.strucmotif.config.InvertedIndexBackend;
import org.rcsb.strucmotif.config.ReadErrorStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.codec.BucketCodec;
import org.rcsb.strucmotif.domain.bucket.ArrayBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The implementation of the inverted index.
 */
@Service
public class DefaultInvertedIndex implements InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(DefaultInvertedIndex.class);
    private static final byte MAGIC = (byte) (1 << 7);
    private static final int EMPTY_INT = Integer.MAX_VALUE;
    private final String extension;
    private final BucketCodec bucketCodec;
    // 'production' data that can be queried
    private final Path rootPath;
    private final Path dataPath;
    private final Path indexPath;
    private ReadableFileBundle fileBundle;
    // paths for 'temporary' bundle written when 'production' data is getting modified
    private final Path temporaryDataPath;
    private final Path temporaryIndexPath;
    private final ReadErrorStrategy readErrorStrategy;

    /**
     * Construct an inverted index instance.
     * @param strucmotifConfig the config
     */
    public DefaultInvertedIndex(StrucmotifConfig strucmotifConfig) {
        InvertedIndexBackend invertedIndexBackend = strucmotifConfig.getInvertedIndexBackend();
        this.bucketCodec = invertedIndexBackend.getBucketCodec();
        this.extension = invertedIndexBackend.getExtension();
        logger.info("Extension of inverted index files: {}", extension);
        this.rootPath = Paths.get(strucmotifConfig.getRootPath());
        this.dataPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.DATA_EXT);
        this.indexPath = rootPath.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.INDEX_EXT);
        this.temporaryDataPath = dataPath.resolveSibling(dataPath.getFileName() + StrucmotifConfig.TMP_EXT);
        this.temporaryIndexPath = indexPath.resolveSibling(indexPath.getFileName() + StrucmotifConfig.TMP_EXT);
        this.readErrorStrategy = strucmotifConfig.getReadErrorStrategy();
    }

    /**
     * Life-cycle operation that runs as part of the initialization.
     * @throws IOException IO operation failed
     */
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

    /**
     * Life-cycle operation that runs during cleanup.
     * @throws IOException IO operation failed
     */
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
            Set<Integer> unchangedDescriptors = Collections.synchronizedSet(reportKnownDescriptors());

            // this captures all additional data
            List<Path> partials = partialFilenames().toList();
            Map<String, List<Path>> sortedByPrefix = partials.stream().collect(Collectors.groupingBy(p -> p.getFileName().toString().split("-")[1].split("\\.")[0]));

            logger.info("Merging partial data from {}", partials);
            WritableFileBundle temporaryFileBundle = initializeTemporaryFileBundle();
            AtomicInteger pathCounter = new AtomicInteger(1);
            for (List<Path> paths : sortedByPrefix.values()) {
                progress(pathCounter, 10, "{} / " + sortedByPrefix.size() + " prefixes processed");
                // outer key: descriptor, inner key: structure index, value: positional data
                Map<Integer, Map<Integer, int[]>> data = Collections.synchronizedMap(new HashMap<>());

                paths.parallelStream().forEach(p -> {
                    try {
                        int currentStructureIndex = EMPTY_INT;
                        int currentDescriptor = EMPTY_INT;
                        int[] array = new int[1024];
                        int arrayPos = 0;
                        byte[] bytes = Files.readAllBytes(p);
                        for (int i = 0; i < bytes.length - 7; i = i + 8) {
                            if ((bytes[i] & MAGIC) != 0) {
                                // looking at metadata
                                if (currentDescriptor != EMPTY_INT) {
                                    int[] actual = Arrays.copyOf(array, arrayPos);
                                    Map<Integer, int[]> map = data.computeIfAbsent(currentDescriptor, e -> Collections.synchronizedMap(new HashMap<>()));
                                    map.put(currentStructureIndex, actual);
                                    // no need to clear out array, will write over old data
                                    arrayPos = 0;
                                }
                                currentStructureIndex = (bytes[i] & ~MAGIC & 0xFF) << 24 | (bytes[i + 1] & 0xFF) << 16 | (bytes[i + 2] & 0xFF) << 8 | (bytes[i + 3] & 0xFF);
                                currentDescriptor = (bytes[i + 4] & 0xFF) << 24 | (bytes[i + 5] & 0xFF) << 16 | (bytes[i + 6] & 0xFF) << 8 | (bytes[i + 7] & 0xFF);
                            } else {
                                // looking at payload
                                if (arrayPos >= array.length) {
                                    array = Arrays.copyOf(array, array.length * 2);
                                }
                                array[arrayPos++] = (bytes[i] & 0xFF) << 24 | (bytes[i + 1] & 0xFF) << 16 | (bytes[i + 2] & 0xFF) << 8 | (bytes[i + 3] & 0xFF);
                                array[arrayPos++] = (bytes[i + 4] & 0xFF) << 24 | (bytes[i + 5] & 0xFF) << 16 | (bytes[i + 6] & 0xFF) << 8 | (bytes[i + 7] & 0xFF);
                            }
                        }

                        if (currentDescriptor == Integer.MAX_VALUE) {
                            // no data
                            return;
                        }

                        // capture last datum
                        int[] actual = Arrays.copyOf(array, arrayPos);
                        Map<Integer, int[]> map = data.computeIfAbsent(currentDescriptor, e -> Collections.synchronizedMap(new HashMap<>()));
                        map.put(currentStructureIndex, actual);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                data.entrySet().parallelStream().forEach(entry -> {
                    // tracking unchanged descriptors to copy them over from prod data at the very end
                    unchangedDescriptors.remove(entry.getKey());

                    int descriptor = entry.getKey();
                    Map<Integer, int[]> perStructureData = entry.getValue();
                    int structureCount = perStructureData.size();
                    int identifierCount = perStructureData.values().stream().mapToInt(Array::getLength).sum();

                    try {
                        int[] structureIndices;
                        int[] positionOffsets;
                        int[] identifierData;
                        int outerPos = 0;
                        int innerPos = 0;

                        // check if there's data in production files, if so concat that to the start of the arrays
                        if (fileBundle.containsFile(descriptor + extension)) {
                            ByteBuffer byteBuffer = fileBundle.readFile(descriptor + extension);
                            ArrayBucket existingBucket = bucketCodec.decode(byteBuffer);

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

                        for (Map.Entry<Integer, int[]> datum : perStructureData.entrySet()) {
                            int structureIndex = datum.getKey();
                            structureIndices[outerPos] = structureIndex;
                            positionOffsets[outerPos] = innerPos;

                            int[] array = datum.getValue();
                            System.arraycopy(array, 0, identifierData, innerPos, array.length);

                            outerPos++;
                            innerPos += array.length;
                        }

                        ArrayBucket bucket = new ArrayBucket(structureIndices, positionOffsets, identifierData);
                        ByteBuffer out = bucketCodec.encode(bucket);
                        temporaryFileBundle.writeFile(descriptor + extension, out);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            fileBundle.close();
            temporaryFileBundle.close();

            if (!unchangedDescriptors.isEmpty()) {
                logger.info("Transferring existing data for {} descriptors", unchangedDescriptors.size());
                Set<String> update = unchangedDescriptors.stream().map(desc -> desc + extension).collect(Collectors.toSet());
                Path selectDataPath = temporaryDataPath.resolveSibling("select.data");
                Path selectIndexPath = temporaryIndexPath.resolveSibling("select.ffindex");
                Files.createFile(selectDataPath);
                Files.createFile(selectIndexPath);

                selectFromBundle(dataPath, indexPath, selectDataPath, selectIndexPath, update);
                FileBundleIO.mergeBundles(temporaryDataPath, temporaryIndexPath, selectDataPath, selectIndexPath);

                Files.deleteIfExists(selectDataPath);
                Files.deleteIfExists(selectIndexPath);
            }

            // delete partial file bundle and swap temporary files with real ones
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            deletePartialFiles();
            initializeFileBundle();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // TODO move to ffindex-java at some point
    record Entry(String filename, long offset, int length) {}

    private static List<Entry> parseEntries(Path indexPath) throws IOException {
        try (Stream<String> lines = Files.lines(indexPath)) {
            return lines.map(line -> line.split("\t"))
                    .map(split -> new Entry(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2])))
                    .toList();
        }
    }

    private static void selectFromBundle(Path dataPath, Path indexPath, Path outputDataPath, Path outputIndexPath, Set<String> selectedFiles) throws IOException {
        List<Entry> originalEntries = parseEntries(indexPath);
        Map<String, Entry> selectedEntries = originalEntries.stream().filter(e -> selectedFiles.contains(e.filename())).collect(Collectors.toMap(Entry::filename, Function.identity()));
        if (selectedEntries.size() != selectedFiles.size()) {
            throw new IllegalStateException("There are missing files in the source bundle - won't manipulate");
        }

        long updatedOffset = 0L;
        StringJoiner updatedIndex = new StringJoiner("\n");
        try (RandomAccessFile originalDataFile = new RandomAccessFile(dataPath.toFile(), "r");
             FileChannel originalData = originalDataFile.getChannel();
             RandomAccessFile additionsDataFile = new RandomAccessFile(outputDataPath.toFile(), "rw");
             FileChannel additionsData = additionsDataFile.getChannel()) {
            for (String filename : selectedFiles) {
                Entry entry = selectedEntries.get(filename);
                ByteBuffer buffer = ByteBuffer.allocate(entry.length());
                originalData.read(buffer, entry.offset());
                buffer.rewind();
                additionsData.write(buffer);
                updatedIndex.add(entry.filename() + "\t" + updatedOffset + "\t" + entry.length());
                updatedOffset += entry.length();
            }
        }
        Files.writeString(outputIndexPath, updatedIndex.toString());
    }

    @Override
    public ArrayBucket select(int residuePairDescriptor) {
        String filename = getFilename(residuePairDescriptor);
        if (!fileBundle.containsFile(filename)) {
            return ArrayBucket.EMPTY_BUCKET;
        }

        try {
            return bucketCodec.decode(getByteBuffer(filename));
        } catch (IOException e) {
            switch (readErrorStrategy) {
                case THROW -> throw new UncheckedIOException("failed to read " + filename, e);
                case EXIT -> {
                    logger.error("Error while reading {} -- terminating process", filename, e);
                    System.exit(74);
                }
                case REINITIALIZE -> {
                    try {
                        logger.error("Error while reading {} -- trying to reinitialize file bundle", filename, e);
                        tearDown();
                        logger.info("Closed file bundle successfully");
                        setUp();
                        logger.info("Re-opened file bundle successfully");
                        return bucketCodec.decode(getByteBuffer(filename));
                    } catch (IOException f) {
                        logger.error("Error while re-initializing file bundle -- terminating process", f);
                        System.exit(74);
                    }
                }
            }
        }
        return null;
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

            // swap new and old files
            fileBundle.close();
            temporaryFileBundle.close();
            Files.move(temporaryDataPath, dataPath, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryIndexPath, indexPath, StandardCopyOption.REPLACE_EXISTING);
            initializeFileBundle();
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
        ArrayBucket bucket = bucketCodec.decode(byteBuffer);
        Set<Integer> structureIndices = bucket.getStructureIndices();

        // if no entry would be removed: don't bother and return
        if (removals.stream().noneMatch(structureIndices::contains)) {
            byteBuffer.rewind();
            return byteBuffer;
        }

        // remove all occurrences of structure identifiers
        ArrayBucket filteredBucket = removeByKey(bucket, removals);
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
        logger.info("Collecting all known keys in bundle ({}, {})", dataPath, indexPath);
        AtomicInteger counter = new AtomicInteger();
        return indexFilenames()
                .parallel()
                .peek(p -> progress(counter, 10000, "{} bins scanned"))
                .map(this::createResiduePairDescriptor)
                .map(this::select)
                .map(ArrayBucket::getStructureIndices)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Stream<String> indexFilenames() {
        return fileBundle.filenames();
    }

    private Stream<Path> partialFilenames() throws IOException {
        List<Path> out;
        try (Stream<Path> paths = Files.list(rootPath)) {
            out = paths.filter(p -> p.getFileName().toString().startsWith(StrucmotifConfig.INDEX) && p.getFileName().toString().endsWith(StrucmotifConfig.TMP_EXT))
                    .toList();
        }
        return out.stream();
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
    private ArrayBucket removeByKey(ArrayBucket bucket, Collection<Integer> removals) {
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

        return new ArrayBucket(structureIndices, positionOffsets, identifierData);
    }
}
