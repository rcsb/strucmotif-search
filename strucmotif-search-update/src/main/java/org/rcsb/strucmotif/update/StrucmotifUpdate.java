package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.ParsingException;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxAuditRevisionHistory;
import org.rcsb.cif.schema.mm.PdbxStructAssembly;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.DefaultThreadPool;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final String FULL = "full";
    private static final String FULL_CSM = "full_csm";

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
     * @param structureIndexProvider index provider
     */
    @Autowired
    public StrucmotifUpdate(StateRepository stateRepository, StructureDataProvider structureDataProvider, InvertedIndex invertedIndex, StrucmotifConfig strucmotifConfig, StructureIndexProvider structureIndexProvider) {
        this.stateRepository = stateRepository;
        this.structureDataProvider = structureDataProvider;
        this.invertedIndex = invertedIndex;
        this.strucmotifConfig = strucmotifConfig;
        this.threadPool = new DefaultThreadPool(strucmotifConfig);
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

        logger.info("Strucmotif config used for loading is:");
        for (Method method : strucmotifConfig.getClass().getMethods()) {
            int modifiers = method.getModifiers();
            String name = method.getName();
            if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers) || !name.startsWith("get") || name.equals("getClass")) {
                continue;
            }

            logger.info("    strucmotif.{}: {}", kebabCase(name.replace("get", "")), method.invoke(strucmotifConfig));
        }

        // determine identifiers requested by user - either provided collection or all currently reported identifiers by RCSB PDB
        Operation operation = parseOperation(args);
        List<UpdateItem> requested = parseUpdateList(args);

        // check for sanity of internal state
        if (operation != Operation.RECOVER) {
            Set<String> dirtyStructureIdentifiers = stateRepository.selectDirty();
            if (dirtyStructureIdentifiers.size() > 0) {
                logger.warn("Update state is dirty - Problematic identifiers:\n{}",
                        dirtyStructureIdentifiers);
                logger.info("Recovering from dirty state");
                recover(dirtyStructureIdentifiers);
            }
        }

        logger.info("Starting update - Operation: {}, {} ids ({})",
                operation,
                requested.size(),
                requested.stream()
                        .limit(5)
                        .map(item -> "\"" + item.getStructureIdentifier() + "\"")
                        .collect(Collectors.joining(", ", "[",  requested.size() > 5 ? ", ...]" : "]")));

        switch (operation) {
            case ADD -> add(new Context(getDeltaPlusIdentifiers(requested)));
            case REMOVE -> remove(getDeltaMinusIdentifiers(requested));
            case RECOVER -> recover(stateRepository.selectDirty());
        }

        logger.info("Finished update operation");
    }

    private static final String RE1 = "([a-z0-9])([A-Z])";
    private static final String RE2 = "([A-Z])([A-Z])(?=[a-z])";
    private static final String REPLACEMENT = "$1-$2";
    private String kebabCase(String s) {
        return s.replaceAll(RE1, REPLACEMENT)
                .replaceAll(RE2, REPLACEMENT)
                .toLowerCase();
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

        Set<String> known = getKnown();

        context.structureCounter = new AtomicInteger();
        threadPool.submit(() -> {
            context.updateItems.parallelStream().forEach(item -> handleUpdateItem(item, context));
            return null;
        }).get();

        // TODO impl
        // TODO allow to commit after some interval?
        // TODO allow to resume after failing?
        commit(context, known);
    }

    private void commit(Context context, Set<String> known) {
        logger.info("Committing data on {} structures", context.processed.size());
        // mark as dirty before index update
        Set<String> dirty = context.processed.stream()
                .map(StructureInformation::structureIdentifier)
                // ignore items detected as known after reading entry.id (happens when processing URLs)
                .filter(id -> !known.contains(id))
                .collect(Collectors.toSet());
        stateRepository.insertDirty(dirty);
        structureDataProvider.commit();
        invertedIndex.commit();

        // processed contains all StructureIdentifiers + corresponding revision
        stateRepository.insertKnown(context.processed);
        stateRepository.deleteDirty(context.processed.stream().map(StructureInformation::structureIdentifier).collect(Collectors.toSet()));

        // reset state
        context.processed.clear();
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
                String structureContext = count + " / " + context.updateItems.size() + "] [" + source;
                logger.warn("[{}] [try: {} / {}] Failed to download or parse source file - {}",
                        structureContext, i, maxRetries, e.getMessage());
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
            MmCifBlock block = mmCifFile.getFirstBlock();

            // assert that all categories are present
            PdbxAuditRevisionHistory revisionHistory = block.getPdbxAuditRevisionHistory();
            if (!revisionHistory.isDefined()) {
                throw new IllegalArgumentException("'pdbx_audit_revision_history' is mandatory in input files - rejecting " + structureIdentifier);
            }
            int majorRevision = revisionHistory.getMajorRevision().get(revisionHistory.getRowCount());
            int minorRevision = revisionHistory.getMinorRevision().get(revisionHistory.getRowCount());
            PdbxStructAssembly pdbxStructAssembly = block.getPdbxStructAssembly();
            PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
            PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
            if (!pdbxStructAssembly.isDefined() || !pdbxStructAssemblyGen.isDefined() || !pdbxStructOperList.isDefined()) {
                // TODO could allow 'lenient' mode that can be used to index AF data directly
                throw new IllegalArgumentException("'pdbx_struct_assembly', 'pdbx_struct_assembly_gen', and 'pdbx_struct_oper_list' are mandatory in input files - rejecting " + structureIdentifier);
            }

            // write renumbered structure
            logger.debug("[{}] Writing renumbered structure file", structureIdentifier);
            structureDataProvider.writeRenumbered(structureIdentifier, mmCifFile);
            context.processed.add(new StructureInformation(structureIdentifier, structureIndex, majorRevision, minorRevision));
        } catch (IOException e) {
            throw new UncheckedIOException("Cif parsing failed for " + structureIdentifier, e);
        } catch (ParsingException e) {
            throw new ParsingException("Cif parsing failed for " + structureIdentifier, e);
        }

        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + strucmotifConfig.getUpdateChunkSize() + "] [" + structureIdentifier;

        // fails when file is missing, expected when e.g. all residues of the file where below the pLDDT threshold
        Structure structure;
        try {
            structure = structureDataProvider.readRenumbered(structureIdentifier);
        } catch (UncheckedIOException e) {
            logger.warn("[{}] No renumbered source file present - Skipping", structureContext);
            return;
        }

        try {
            long start = System.nanoTime();
            // TODO include whole chain in contact? 'all' is favorable but wastes a lot of space
            ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

            // extract motifs
            AtomicInteger structureMotifCounter = new AtomicInteger();
            Map<Integer, List<ResiduePairOccurrence>> output = residueGraph.residuePairOccurrencesSequential()
                    .collect(Collectors.groupingBy(ResiduePairOccurrence::getResiduePairDescriptor));
            // TODO write partial outputs
//                    .forEach(residuePairOccurrence -> {
//                        try {
//                            indexWriter.write(residuePairOccurrence.getResiduePairDescriptor() + " " + residuePairOccurrence.getResiduePairIdentifier() + "\n");
//                            structureMotifCounter.incrementAndGet();
//                        } catch (IOException e) {
//                            throw new UncheckedIOException(e);
//                        }
//                    });
            logger.info("[{}] Extracted {} residue pairs in {} ms",
                    structureContext,
                    structureMotifCounter.get(),
                    (System.nanoTime() - start) / 1000 / 1000);
        } catch (Exception e) {
            logger.warn("[{}] Residue graph determination failed",
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
            logger.info("[{}] Processing {}", item.getStructureIdentifier(), url);
            return url.openStream();
        }

        return structureDataProvider.getOriginalInputStream(item.getStructureIdentifier());
    }

//    private void writeTemporaryFiles(Context context) throws ExecutionException, InterruptedException {
//        logger.info("[{}] Writing temporary files for {} residue pair descriptors",
//                context.partitionContext,
//                context.buffer.size());
//
//        final int bufferTotal = context.buffer.size();
//        AtomicInteger bufferCount = new AtomicInteger();
//        threadPool.submit(() -> {
//            context.buffer.keySet().parallelStream().forEach(key -> {
//                Map<Integer, Collection<ResiduePairIdentifier>> value = context.buffer.remove(key);
//                ResiduePairIdentifierBucket output = new ResiduePairIdentifierBucket(value);
//
//                if (bufferCount.incrementAndGet() % 10000 == 0) {
//                    logger.info("[{}] {} / {}",
//                            context.partitionContext,
//                            bufferCount,
//                            bufferTotal);
//                }
//
//                invertedIndex.insert(key, output, context.batchId.get());
//            });
//            return null;
//        }).get();
//
//        context.buffer.clear();
//
//        // increment tmp file counter
//        context.batchId.incrementAndGet();
//    }

    /**
     * 'REMOVE' operation.
     * @param identifierList set of identifiers to remove
     */
    public void remove(Collection<String> identifierList) {
        Set<String> identifiers = new HashSet<>(identifierList);
        // mark everything that will be touched as dirty in case this operation fails
        stateRepository.insertDirty(identifiers);

        // modifying the renumbered file bundle is expensive and is done as batch
        logger.info("Removing {} renumbered structure", identifiers.size());
        structureDataProvider.deleteRenumbered(identifiers);
        logger.debug("Done removing renumbered structure");

        // modifying the inverted index is expensive and is done as batch
        if (!identifiers.isEmpty()) {
            Set<Integer> mapped = identifiers.stream()
                    .filter(structureIndexProvider::containsKey)
                    .map(structureIndexProvider::selectStructureIndex)
                    .collect(Collectors.toSet());
            if (!mapped.isEmpty()) {
                logger.info("Removing {} structures from inverted index", mapped.size());
                invertedIndex.delete(mapped);
                logger.debug("Done removing structures from inverted index");
            }
        }

        logger.info("Updating holdings");
        stateRepository.deleteKnown(identifiers);
        stateRepository.deleteDirty(identifiers);
        logger.info("Finished removal operation");
    }

    /**
     * Reports all structures currently present in the PDB archive.
     * @return collection of update items
     * @throws IOException connection failure
     */
    public List<UpdateItem> getAllIdentifiers(String mode) throws IOException {
        logger.info("Retrieving current {} entry list from RCSB PDB Search API", mode);
        List<UpdateItem> out = new ArrayList<>();
        try (InputStream inputStream = composeSearchUrl(mode).openStream()) {
            JsonElement jsonElement = new Gson().fromJson(new InputStreamReader(inputStream), JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            jsonObject.getAsJsonArray("result_set")
                    .forEach(id -> out.add(new UpdateItem(id.getAsString())));
        }
        return out;
    }

    private URL composeSearchUrl(String mode) throws MalformedURLException {
        String types = switch (mode) {
            case FULL -> "\"experimental\"";
            case FULL_CSM -> "\"computational\", \"experimental\"";
            default -> throw new UnsupportedOperationException(mode + " is not yet implemented");
        };

        String query = URLEncoder.encode("{\n" +
                "  \"query\": {\n" +
                        "    \"type\": \"terminal\",\n" +
                        "    \"label\": \"text\",\n" +
                        "    \"service\": \"text\",\n" +
                        "    \"parameters\": {\n" +
                        "      \"attribute\": \"rcsb_entry_container_identifiers.entry_id\",\n" +
                        "      \"operator\": \"exists\",\n" +
                        "      \"negation\": false\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"return_type\": \"entry\",\n" +
                        "  \"request_options\": {\n" +
                        "    \"results_content_type\": [\n" +
                        types +
                        "    ],\n" +
                        "    \"return_all_hits\": true,\n" +
                        "    \"results_verbosity\": \"compact\"\n" +
                        "  }\n" +
                        "}", StandardCharsets.UTF_8);
        return new URL("https://search.rcsb.org/rcsbsearch/v2/query?json=" + query);
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @return UpdateItem collection that need to be processed for the given context
     */
    public List<UpdateItem> getDeltaPlusIdentifiers(List<UpdateItem> requested) {
        Set<String> known = getKnown();
        if (known.isEmpty()) {
            logger.warn("No existing data - Starting from scratch");
            return requested;
        } else {
            return requested.stream()
                    .filter(item -> !known.contains(item.getStructureIdentifier()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param requested the requested update
     * @return UpdateItem collection that need to be removed for the given context
     */
    public List<String> getDeltaMinusIdentifiers(List<UpdateItem> requested) {
        Set<String> known = getKnown();
        if (known.isEmpty()) {
            logger.warn("No existing data - no need for cleanup of obsolete entries");
            return Collections.emptyList();
        } else {
            Set<String> unwrapped = requested.stream()
                    .map(UpdateItem::getStructureIdentifier)
                    .collect(Collectors.toSet());
            return known.stream()
                    .filter(unwrapped::contains)
                    .collect(Collectors.toList());
        }
    }

    private Set<String> getKnown() {
        return stateRepository.selectKnown()
                .stream()
                .map(StructureInformation::structureIdentifier)
                .collect(Collectors.toSet());
    }

    private void recover(Set<String> dirty) {
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
    }

    private void printUsage() {
        System.out.println("Too few arguments");
        System.out.println();
        System.out.println("Usage: java -Xmx12G -jar update.jar operation ...");
        System.out.println("Valid operation values: " + Arrays.toString(Operation.values()));
        System.out.println("Optionally: list of entry ids - (no argument performs null operation, use single argument '" + FULL + "' for complete update based on RCSB PDB holdings, use '" + FULL_CSM + "' to include computed structure models from AlphaFold DB)");
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

    private static final String PDB_REGEX = "^[1-9][a-zA-Z0-9]{3}$"; // TODO need to revisit for extended PDB-IDs
    private static final String CSM_REGEX = "^[a-zA-Z0-9]+_[a-zA-Z0-9]{6,}$"; // pattern used by rcsb.org for computed structure models
    private List<UpdateItem> parseUpdateList(String[] args) throws IOException {
        int offset = 1;
        String[] ids = new String[args.length - offset];
        List<UpdateItem> requested;
        System.arraycopy(args, offset, ids, 0, ids.length);
        if (ids.length == offset && (ids[0].equalsIgnoreCase(FULL) || ids[0].equalsIgnoreCase(FULL_CSM))) {
            requested = getAllIdentifiers(ids[0].toLowerCase());
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
                        if (id.matches(PDB_REGEX) || id.matches(CSM_REGEX)) {
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
