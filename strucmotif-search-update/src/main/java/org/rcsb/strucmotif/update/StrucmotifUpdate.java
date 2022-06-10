package org.rcsb.strucmotif.update;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.ParsingException;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.domain.bucket.ResiduePairIdentifierBucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.AssemblyInformation;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Revision;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.math.Partition;
import org.rcsb.strucmotif.update.extractor.KeyExtractorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.depositedAndContacts;

/**
 * Runs strucmotif updates from the command-line.
 */
@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@ComponentScan({"org.rcsb.strucmotif"})
@EntityScan("org.rcsb.strucmotif")
public class StrucmotifUpdate implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StrucmotifUpdate.class);
    private static final Set<String> STRUCTURE_EXTENSIONS = Set.of(".cif", ".cif.gz", ".bcif", ".bcif.gz");

    /**
     * Entry point from the command-line.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(StrucmotifUpdate.class, args);
    }

    private final StateRepository stateRepository;
    private final StructureDataProvider structureDataProvider;
    private final InvertedIndex invertedIndex;
    private final StrucmotifConfig strucmotifConfig;
    private final ThreadPool threadPool;
    private final StructureIndexProvider structureIndexProvider;

    /**
     * Injectable constructor.
     * @param stateRepository the state repo
     * @param structureDataProvider data provider
     * @param invertedIndex inverted index
     * @param strucmotifConfig configs
     * @param threadPool thread pool
     * @param structureIndexProvider index provider
     */
    @Autowired
    public StrucmotifUpdate(StateRepository stateRepository, StructureDataProvider structureDataProvider, InvertedIndex invertedIndex, StrucmotifConfig strucmotifConfig, ThreadPool threadPool, StructureIndexProvider structureIndexProvider) {
        this.stateRepository = stateRepository;
        this.structureDataProvider = structureDataProvider;
        this.invertedIndex = invertedIndex;
        this.strucmotifConfig = strucmotifConfig;
        this.threadPool = threadPool;
        this.structureIndexProvider = structureIndexProvider;
    }

    /**
     * Actual run method with the given arguments.
     * @param args command-line arguments
     * @throws Exception update failure
     */
    public void run(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            return;
        }

        // determine identifiers requested by user - either provided collection or all currently reported identifiers by RCSB PDB
        Operation operation = parseOperation(args);
        List<UpdateItem> requested = parseUpdateList(args);

        // check for sanity of internal state
        if (operation != Operation.RECOVER) {
            Collection<String> dirtyStructureIdentifiers = stateRepository.selectDirty();
            if (dirtyStructureIdentifiers.size() > 0) {
                logger.warn("Update state is dirty - Problematic identifiers:\n{}",
                        dirtyStructureIdentifiers);
                logger.info("Recovering from dirty state");
                recover(dirtyStructureIdentifiers);
            }

            logger.info("Making sure that no temporary files linger");
            invertedIndex.clearTemporaryFiles();
        }

        logger.info("Starting update - Operation: {}, {} ids ({})",
                operation,
                requested.size(),
                requested.stream()
                        .limit(5)
                        .map(item -> "\"" + item.getStructureIdentifier() + "\"")
                        .collect(Collectors.joining(", ", "[",  requested.size() > 5 ? ", ...]" : "]")));

        switch (operation) {
            case ADD:
                add(new Context(getDeltaPlusIdentifiers(requested), parseBatchId()));
                break;
            case REMOVE:
                remove(getDeltaMinusIdentifiers(requested));
                break;
            case RECOVER:
                recover(stateRepository.selectDirty());
                break;
        }

        logger.info("Finished update operation");
    }

    private UpdateItem mapFile(Path path) {
        try {
            return new UpdateItem(KeyExtractorFactory.getKey(path.toFile().getName()), path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The 'ADD' operation.
     * @param context the set of UpdateItems to add
     * @throws ExecutionException update failure
     * @throws InterruptedException update failure
     */
    public void add(Context context) throws ExecutionException, InterruptedException {
        long target = context.updateItems.size();
        logger.info("{} files to process in total", target);

        Partition<UpdateItem> partitions = new Partition<>(context.updateItems, strucmotifConfig.getUpdateChunkSize());
        logger.info("Formed {} partitions of {} structures",
                partitions.size(),
                strucmotifConfig.getUpdateChunkSize());

        Collection<String> known = getKnown();
        boolean needsCommit = false;

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            context.partitionContext = (i + 1) + " / " + partitions.size();

            List<UpdateItem> partition = partitions.get(i);
            logger.info("[{}] Start processing partition", context.partitionContext);

            context.structureCounter = new AtomicInteger();
            context.buffer = new ConcurrentHashMap<>();
            threadPool.submit(() -> {
                partition.parallelStream().forEach(item -> handleUpdateItem(item, context));
                return null;
            }).get();

            writeTemporaryFiles(context);
            needsCommit = true;

            if (i > 0 && i % strucmotifConfig.getCommitInterval() == 0) {
                commit(context, known);
                needsCommit = false;
            }
        }

        if (needsCommit) {
            commit(context, known);
        }
    }

    private void commit(Context context, Collection<String> known) {
        // mark as dirty before index update
        Set<String> dirty = context.processed.stream()
                .map(StructureInformation::getStructureIdentifier)
                // ignore items detected as known after reading entry.id (happens when processing URLs)
                .filter(id -> !known.contains(id))
                .collect(Collectors.toSet());
        stateRepository.insertDirty(dirty);
        invertedIndex.commit();

        // processed contains all StructureIdentifiers + corresponding revision
        stateRepository.insertKnown(context.processed);
        stateRepository.deleteDirty(context.processed.stream().map(StructureInformation::getStructureIdentifier).collect(Collectors.toSet()));

        // reset state
        context.processed.clear();
        context.batchId.set(0);
    }

    private void handleUpdateItem(UpdateItem item, Context context) {
        int maxRetries = strucmotifConfig.getDownloadTries();
        for (int i = 1; i <= maxRetries; i++) {
            try {
                handleUpdateItemInternal(item, context);
                break;
            } catch (UncheckedIOException | ParsingException e) {
                if (i >= maxRetries) {
                    // max retries exceeded
                    throw e;
                }

                int count = context.structureCounter.get();
                String source = item.getUrl() != null ? item.getUrl().toString() : item.getStructureIdentifier();
                String structureContext = count + " / " + strucmotifConfig.getUpdateChunkSize() + "] [" + source;
                logger.warn("[{}] [{}] [try: {} / {}] Failed to download or parse source file - {}",
                        context.partitionContext,
                        structureContext,
                        i,
                        maxRetries,
                        e.getMessage());
            }
        }
    }

    private void handleUpdateItemInternal(UpdateItem item, Context context) {
        String structureIdentifier = item.getStructureIdentifier();
        int structureIndex = structureIndexProvider.nextStructureIndex();
        try {
            InputStream inputStream = handleInputStream(item, context);

            // get some clean metadata
            MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

            boolean hasRevision = mmCifFile.getFirstBlock().getPdbxAuditRevisionHistory().isDefined();
            // if revision isn't set (happens e.g. for ModelArchive files) then set to 1.0 by default
            Revision revision = hasRevision ? new Revision(mmCifFile) : new Revision(1, 0);
            Map<String, String[]> assemblyInformation = AssemblyInformation.of(mmCifFile);

            // write renumbered structure
            structureDataProvider.writeRenumbered(structureIdentifier, mmCifFile);
            context.processed.add(new StructureInformation(structureIdentifier, structureIndex, revision, assemblyInformation));
        } catch (IOException e) {
            throw new UncheckedIOException("Cif parsing failed for " + structureIdentifier, e);
        } catch (ParsingException e) {
            throw new ParsingException("Cif parsing failed for " + structureIdentifier, e);
        }

        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + strucmotifConfig.getUpdateChunkSize() + "] [" + structureIdentifier;

        // fails when file is missing (should not happen) or does not contain valid polymer chain
        Structure structure;
        try {
            structure = structureDataProvider.readRenumbered(structureIdentifier);
        } catch (UncheckedIOException e) {
            logger.warn("[{}] [{}] No valid polymer chain(s) - Skipping",
                    context.partitionContext,
                    structureContext);
            return;
        }

        if (!strucmotifConfig.isUndefinedAssemblies() && structure.getAssemblies().isEmpty()) {
            logger.warn("[{}] [{}] No assembly information - Configured to skip",
                    context.partitionContext,
                    structureContext);
            return;
        }

        try {
            long start = System.nanoTime();
            ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

            // extract motifs
            AtomicInteger structureMotifCounter = new AtomicInteger();
            threadPool.submit(() -> {
                residueGraph.residuePairOccurrencesParallel()
                        .forEach(motifOccurrence -> {
                            ResiduePairDescriptor motifDescriptor = motifOccurrence.getResiduePairDescriptor();
                            ResiduePairIdentifier targetIdentifier = motifOccurrence.getResidueIdentifier();

                            Map<Integer, Collection<ResiduePairIdentifier>> groupedTargetIdentifiers = context.buffer.computeIfAbsent(motifDescriptor, k -> Collections.synchronizedMap(new HashMap<>()));
                            Collection<ResiduePairIdentifier> targetIdentifiers = groupedTargetIdentifiers.computeIfAbsent(structureIndex, k -> Collections.synchronizedSet(new HashSet<>()));
                            targetIdentifiers.add(targetIdentifier);
                            structureMotifCounter.incrementAndGet();
                        });
                return null;
            }).get();
            logger.info("[{}] [{}] Extracted {} residue pairs in {} ms",
                    context.partitionContext,
                    structureContext,
                    structureMotifCounter.get(),
                    (System.nanoTime() - start) / 1000 / 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("[{}] [{}] Residue graph determination failed",
                    context.partitionContext,
                    structureContext,
                    e);
            // fail complete update
            throw new RuntimeException("Residue graph determination failed for " + structureIdentifier, e);
        }
    }

    /**
     * Acquire an input stream for the requested item. Simple case is a 4-character PDB-ID. Might also be a URL.
     * @param item request
     * @param context context for logging purposes
     * @throws IOException when reading fails
     * @return an InputStream
     */
    protected InputStream handleInputStream(UpdateItem item, Context context) throws IOException {
        URL url = item.getUrl();
        if (url != null) {
            logger.info("[{}] [{}] Processing {}", context.partitionContext, item.getStructureIdentifier(), url);
            return url.openStream();
        }

        return structureDataProvider.getOriginalInputStream(item.getStructureIdentifier());
    }

    private void writeTemporaryFiles(Context context) throws ExecutionException, InterruptedException {
        logger.info("[{}] Writing temporary files for {} residue pair descriptors",
                context.partitionContext,
                context.buffer.size());

        final int bufferTotal = context.buffer.size();
        AtomicInteger bufferCount = new AtomicInteger();
        threadPool.submit(() -> {
            context.buffer.entrySet().parallelStream().forEach(entry -> {
                ResiduePairDescriptor key = entry.getKey();
                ResiduePairIdentifierBucket output = new ResiduePairIdentifierBucket(entry.getValue());

                if (bufferCount.incrementAndGet() % 10000 == 0) {
                    logger.info("[{}] {} / {}",
                            context.partitionContext,
                            bufferCount,
                            bufferTotal);
                }

                invertedIndex.insert(key, output, context.batchId.get());
            });
            return null;
        }).get();

        context.buffer.clear();

        // increment tmp file counter
        context.batchId.incrementAndGet();
    }

    /**
     * 'REMOVE' operation.
     * @param identifiers set of identifiers to remove
     */
    public void remove(Collection<String> identifiers) {
        // mark everything that will be touched as dirty in case this operation fails
        stateRepository.insertDirty(identifiers);

        AtomicInteger counter = new AtomicInteger();
        for (String structureIdentifier : identifiers) {
            logger.info("[{}] Removing renumbered structure for entry: {}",
                    counter.incrementAndGet() + " / " + identifiers.size(),
                    structureIdentifier);
            structureDataProvider.deleteRenumbered(structureIdentifier);
        }

        // inverted index is expensive and should be done as batch
        if (!identifiers.isEmpty()) {
            Set<Integer> mapped = identifiers.stream()
                    .filter(structureIndexProvider::containsKey)
                    .map(structureIndexProvider::selectStructureIndex)
                    .collect(Collectors.toSet());
            if (!mapped.isEmpty()) {
                invertedIndex.delete(mapped);
            }
        }

        stateRepository.deleteKnown(identifiers);
        stateRepository.deleteDirty(identifiers);
        logger.info("Finished removal operation");
    }

    private static final Pattern ENTRY_ID_PATTERN = Pattern.compile("[0-9][0-9A-Z]{3}");

    /**
     * Reports all structures currently present in the PDB archive.
     * @return collection of update items
     * @throws IOException connection failure
     */
    public List<UpdateItem> getAllIdentifiers() throws IOException {
        String rcsbEntryHoldingsUrl = strucmotifConfig.getRcsbEntryHoldingsUrl();
        logger.info("Retrieving current entry list from {}", rcsbEntryHoldingsUrl);
        String response;
        try (InputStream inputStream = new URL(rcsbEntryHoldingsUrl).openStream()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                response = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
        return ENTRY_ID_PATTERN.matcher(response)
                .results()
                .map(MatchResult::group)
                .map(String::toUpperCase)
                .map(UpdateItem::new)
                .collect(Collectors.toList());
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @return UpdateItem collection that need to be processed for the given context
     */
    public Collection<UpdateItem> getDeltaPlusIdentifiers(Collection<UpdateItem> requested) {
        Collection<String> known = getKnown();
        if (known.isEmpty()) {
            logger.warn("No existing data - Starting from scratch");
            return requested;
        } else {
            return requested.stream()
                    .filter(item -> !known.contains(item.getStructureIdentifier()))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param requested the requested update
     * @return UpdateItem collection that need to be removed for the given context
     */
    public Collection<String> getDeltaMinusIdentifiers(Collection<UpdateItem> requested) {
        Collection<String> known = getKnown();
        if (known.isEmpty()) {
            logger.warn("No existing data - no need for cleanup of obsolete entries");
            return Collections.emptySet();
        } else {
            Collection<String> unwrapped = requested.stream()
                    .map(UpdateItem::getStructureIdentifier)
                    .collect(Collectors.toSet());
            return known.stream()
                    .filter(unwrapped::contains)
                    .collect(Collectors.toSet());
        }
    }

    private Collection<String> getKnown() {
        return stateRepository.selectKnown()
                .stream()
                .map(StructureInformation::getStructureIdentifier)
                .collect(Collectors.toSet());
    }

    private void recover(Collection<String> dirty) {
        remove(dirty);

        // this will happen when writing to the inverted index fails: then bins can be corrupted and filled with structure indices that point nowhere
        logger.info("Screening for lingering structures in the index");
        Set<Integer> knownToIndex = invertedIndex.reportKnownKeys();
        Set<Integer> knownToState = stateRepository.reportKnownKeys();
        Set<Integer> lingeringInIndex = knownToIndex.stream()
                .filter(i -> !knownToState.contains(i))
                .collect(Collectors.toSet());
        if (!lingeringInIndex.isEmpty()) {
            logger.info("{} lingering keys detected - removing...", lingeringInIndex.size());
            invertedIndex.delete(lingeringInIndex);
        }

        logger.info("Making sure that no temporary files linger");
        invertedIndex.clearTemporaryFiles();
    }

    private void printUsage() {
        System.out.println("Too few arguments");
        System.out.println();
        System.out.println("Usage: java -Xmx12G -jar update.jar operation ...");
        System.out.println("Valid operation values: " + Arrays.toString(Operation.values()));
        System.out.println("Optionally: list of entry ids - (no argument performs null operation, use single argument 'full' for complete update)");
        System.out.println("If you want to update entries you have to explicitly remove them first");
        System.out.println();
        System.out.println("Example: java -Xmx12G -jar update.jar ADD 1acj 1exr 4hhb");
        System.out.println("Example: java -Xmx12G -jar update.jar ADD path /opt/data/pdb/");
        System.out.println();
        System.out.println("You can also provide URLs to index non-archived CIF files, in that case you must provide a unique, preferably namespaced identifier which will be used to index this item");
        System.out.println("Example: java -Xmx12G -jar update.jar ADD AF-Q76EI6-F1,https://alphafold.ebi.ac.uk/files/AF-Q76EI6-F1-model_v1.cif MA-9Z55Z,file:///path/to/ma-9z55z.cif");
    }

    private Operation parseOperation(String[] args) {
        return Operation.resolve(args[0]);
    }

    private AtomicInteger parseBatchId() {
        return new AtomicInteger();
    }

    private List<UpdateItem> parseUpdateList(String[] args) throws IOException {
        int offset = 1;
        String[] ids = new String[args.length - offset];
        List<UpdateItem> requested;
        System.arraycopy(args, offset, ids, 0, ids.length);
        if (ids.length == offset && ids[0].equalsIgnoreCase("full")) {
            requested = getAllIdentifiers();
        } else if (ids.length == offset + 1 && ids[0].equalsIgnoreCase("path")) {
            try (Stream<Path> paths = Files.walk(Paths.get(ids[offset]))) {
                requested = paths.filter(path -> STRUCTURE_EXTENSIONS.stream().anyMatch(ext -> path.toFile().getName().toLowerCase().endsWith(ext)))
                        .map(this::mapFile)
                        .collect(Collectors.toList());
            }
        } else {
            requested = Arrays.stream(ids)
                    // upper-case PDB-IDs, leave URLs be
                    .map(id -> {
                        String[] split = id.split(",");
                        if (id.length() == 4) {
                            return new UpdateItem(id.toUpperCase());
                        } else if (split.length == 2) {
                            try {
                                String key = split[0].toUpperCase();
                                URL url = new URL(split[1]);
                                return new UpdateItem(key, url);
                            } catch (MalformedURLException e) {
                                throw new IllegalArgumentException("Cannot parse line: '" + id + "' - not a valid URL");
                            }
                        } else {
                            throw new IllegalArgumentException("Cannot parse line: '" + id + "' - format is '${4-digit-entryId}' or '${identifier},${url}'");
                        }
                    })
                    .collect(Collectors.toList());
        }
        Collections.shuffle(requested);

        return requested;
    }
}
