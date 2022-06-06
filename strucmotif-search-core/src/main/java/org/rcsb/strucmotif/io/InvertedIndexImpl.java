package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.config.InvertedIndexBackend;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.io.codec.BucketCodec;
import org.rcsb.strucmotif.domain.bucket.ResiduePairIdentifierBucket;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The implementation of the inverted index.
 */
@Service
public class InvertedIndexImpl implements InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndexImpl.class);
    private static final Map<String, ResidueType> OLC_LOOKUP = Stream.of(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getInternalCode, Function.identity()));
    private static final int BUFFER_SIZE = 65536;
    private final Path basePath;
    private final boolean gzipped;
    private final String extension;
    private final BucketCodec bucketCodec;
    private final Pattern tempExtension;
    private boolean paths;

    /**
     * Construct an inverted index instance.
     * @param strucmotifConfig the config
     */
    public InvertedIndexImpl(StrucmotifConfig strucmotifConfig) {
        this.basePath = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.INDEX_DIRECTORY);
        this.gzipped = strucmotifConfig.isInvertedIndexGzip();
        InvertedIndexBackend backend = strucmotifConfig.getInvertedIndexBackend();
        this.bucketCodec = backend.getBucketCodec();
        this.extension = backend.getExtension() + (gzipped ? ".gz" : "");
        logger.info("Index files will {}be gzipped - extension: {}", gzipped ? "" : "not ", extension);
        this.tempExtension = Pattern.compile(".*" + extension.replace(".", "\\.") + "\\.[0-9]+$");
        this.paths = false;
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Bucket bucket, int batchId) {
        if (!paths) {
            ensureDirectoriesExist();
            this.paths = true;
        }

        try {
            // write a temporary file (appended by the batchId)
            Path tmpPath = getPath(residuePairDescriptor, batchId);
            try (ByteArrayOutputStream outputStream = bucketCodec.encode(bucket)) {
                write(tmpPath, outputStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void commit() {
        try {
            Map<Path, Set<Path>> toMerge = new ConcurrentHashMap<>();
            try (Stream<Path> paths = temporaryFiles()) {
                paths.forEach(p -> {
                    String filename = p.getFileName().toString();
                    Path persistentPath = p.resolveSibling(filename.substring(0, filename.lastIndexOf(".")));
                    Set<Path> bin = toMerge.computeIfAbsent(persistentPath, e -> Collections.synchronizedSet(new HashSet<>()));
                    bin.add(persistentPath.resolveSibling(p));
                });
            }

            logger.info("Merging {} bins", toMerge.size());

            toMerge.entrySet()
                    .parallelStream()
                    .forEach(entry -> {
                        Path destination = entry.getKey();
                        Set<Path> sources = entry.getValue();

                        try {
                            Map<Integer, Collection<ResiduePairIdentifier>> merged = new HashMap<>();
                            // populate with persistent data
                            if (Files.exists(destination)) {
                                addAll(merged, bucketCodec.decode(getInputStream(destination)),  null);
                            }

                            // merge all new, temporary data
                            for (Path p : sources) {
                                addAll(merged, bucketCodec.decode(getInputStream(p)), null);
                            }
                            ByteArrayOutputStream outputStream = bucketCodec.encode(new ResiduePairIdentifierBucket(merged));
                            write(destination, outputStream);

                            for (Path source : sources) {
                                Files.delete(source);
                            }
                            logger.debug("Merged {} into {}", sources, destination);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void clearTemporaryFiles() {
        try {
            logger.info("Collecting all temporary index files at {}", basePath);
            AtomicInteger counter = new AtomicInteger();
            try (Stream<Path> paths = temporaryFiles()) {
                paths.peek(p -> progress(counter, 10000, "{} files deleted"))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void write(Path path, ByteArrayOutputStream data) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            OutputStream actual = gzipped ? new GZIPOutputStream(outputStream, BUFFER_SIZE) : outputStream;
            data.writeTo(actual);
            actual.flush();
            actual.close();
        }
    }

    @Override
    public InvertedIndexBucket select(ResiduePairDescriptor residuePairDescriptor) {
        try (InputStream inputStream = getInputStream(residuePairDescriptor)) {
            // PSE can cause identifiers to flip - if so we need to flip them again to ensure correct overlap with other words
            return bucketCodec.decode(inputStream);
        } catch (IOException e) {
            return InvertedIndexBucket.EMPTY_BUCKET;
        }
    }

    /**
     * Acquire the input stream for a descriptor.
     * @param residuePairDescriptor the descriptor of interest
     * @return the corresponding input stream
     * @throws IOException reading failed
     */
    protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
        return getInputStream(getPath(residuePairDescriptor));
    }

    private InputStream getInputStream(Path path) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        return gzipped ? new GZIPInputStream(inputStream, BUFFER_SIZE) : new BufferedInputStream(inputStream, BUFFER_SIZE);
    }

    private Path getPath(ResiduePairDescriptor residuePairDescriptor) {
        String bin = residuePairDescriptor.toString();
        String uberbin = bin.substring(0, 2);
        return basePath.resolve(uberbin).resolve(bin + extension);
    }

    private Path getPath(ResiduePairDescriptor residuePairDescriptor, int batchId) {
        Path actual = getPath(residuePairDescriptor);
        return actual.resolveSibling(actual.getFileName().toString() + "." + batchId);
    }

    private InvertedIndexBucket getBucket(ResiduePairDescriptor residuePairDescriptor) {
        try (InputStream inputStream = getInputStream(residuePairDescriptor)) {
            return bucketCodec.decode(inputStream);
        } catch (IOException e) {
            return InvertedIndexBucket.EMPTY_BUCKET;
        }
    }

    @Override
    public void delete(Collection<Integer> removals) {
        try {
            logger.info("Removing {} structures from inverted index", removals.size());

            AtomicInteger counter = new AtomicInteger();
            // walk whole lookup
            indexFiles()
                    .peek(path -> progress(counter, 10000, "{} bins of inverted index cleaned"))
                    .map(this::createResiduePairDescriptor)
                    .forEach(residuePairDescriptor -> delete(residuePairDescriptor, removals));
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

    private ResiduePairDescriptor createResiduePairDescriptor(Path path) {
        String name = path.toFile().getName();
        String[] split = name.split("\\.")[0].split("-");
        ResidueType residueType1 = OLC_LOOKUP.getOrDefault(split[0].substring(0, 1), null);
        ResidueType residueType2 = OLC_LOOKUP.getOrDefault(split[0].substring(1, 2), null);
        DistanceType d1 = DistanceType.ofIntRepresentation(Integer.parseInt(split[1]));
        DistanceType d2 = DistanceType.ofIntRepresentation(Integer.parseInt(split[2]));
        AngleType a = AngleType.ofIntRepresentation(Integer.parseInt(split[3]));
        return new ResiduePairDescriptor(residueType1, residueType2, d1, d2, a);
    }

    private void delete(ResiduePairDescriptor residuePairDescriptor, Collection<Integer> removals) {
        try {
            InvertedIndexBucket bucket = getBucket(residuePairDescriptor);
            Set<Integer> structureIndices = bucket.getStructureIndices();

            // if no entry would be removed: don't bother and return
            if (removals.stream().noneMatch(structureIndices::contains)) {
                return;
            }

            // remove all occurrences of structure identifiers
            ResiduePairIdentifierBucket filteredBucket = removeByKey(bucket, removals);

            // serialize message
            try (ByteArrayOutputStream outputStream = bucketCodec.encode(filteredBucket)) {
                Path path = getPath(residuePairDescriptor);
                write(path, outputStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void ensureDirectoriesExist() {
        try {
            List<String> oneLetterCodes = Stream.of(ResidueType.values())
                    .map(ResidueType::getInternalCode)
                    // has to be sorted to honor the implicit contract on when identifiers are flipped
                    .sorted()
                    .collect(Collectors.toList());

            // create all combinations of one-letter-codes
            for (int i = 0; i < oneLetterCodes.size(); i++) {
                for (int j = i; j < oneLetterCodes.size(); j++) {
                    Path dir = basePath.resolve(oneLetterCodes.get(i) + oneLetterCodes.get(j));

                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<ResiduePairDescriptor> reportKnownDescriptors() {
        try {
            logger.info("Collecting all known descriptors at {}", basePath);
            AtomicInteger counter = new AtomicInteger();
            return indexFiles()
                    .peek(p -> progress(counter, 10000, "{} bins scanned"))
                    .map(this::createResiduePairDescriptor)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Set<Integer> reportKnownKeys() {
        try {
            logger.info("Collecting all known keys at {}", basePath);
            AtomicInteger counter = new AtomicInteger();
            return indexFiles()
                    .peek(p -> progress(counter, 10000, "{} bins scanned"))
                    .map(this::createResiduePairDescriptor)
                    .map(this::getBucket)
                    .map(Bucket::getStructureIndices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Stream<Path> indexFiles() throws IOException {
        return files().filter(p -> p.getFileName().toString().endsWith(extension));
    }

    private Stream<Path> temporaryFiles() throws IOException {
        return files().filter(p -> tempExtension.matcher(p.getFileName().toString()).matches());
    }

    private Stream<Path> allFiles() throws IOException {
        return files().filter(p -> p.getFileName().toString().contains(extension));
    }

    private Stream<Path> files() throws IOException {
        if (!Files.exists(basePath)) {
            return Stream.empty();
        }

        return Files.walk(basePath, FileVisitOption.FOLLOW_LINKS)
                .parallel()
                .filter(Files::isRegularFile);
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
        return new ResiduePairIdentifierBucket(map);
    }
}
