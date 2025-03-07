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
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final StructureIndexProvider structureIndexProvider;
    private final ResidueGraph.ResidueGraphOptions residueGraphOptions;

    /**
     * Injectable constructor.
     * @param stateRepository the state repo
     * @param structureDataProvider data provider
     * @param invertedIndex inverted index
     * @param strucmotifConfig configs
     * @param structureIndexProvider index provider
     */
    @Autowired
    public StrucmotifUpdate(StateRepository stateRepository, StructureDataProvider structureDataProvider, InvertedIndex invertedIndex, StrucmotifConfig strucmotifConfig, StructureIndexProvider structureIndexProvider) throws IOException {
        this.stateRepository = stateRepository;
        this.structureDataProvider = structureDataProvider;
        structureDataProvider.enterWriteMode();
        this.invertedIndex = invertedIndex;
        this.strucmotifConfig = strucmotifConfig;
        this.structureIndexProvider = structureIndexProvider;
        switch (strucmotifConfig.getResidueGraphStrategy()) {
            case DEPOSITED -> this.residueGraphOptions = ResidueGraph.ResidueGraphOptions.deposited();
            case RESIDUES_IN_CONTACT -> this.residueGraphOptions = ResidueGraph.ResidueGraphOptions.residuesInContact();
            case CHAINS_IN_CONTACT -> this.residueGraphOptions = ResidueGraph.ResidueGraphOptions.chainsInContact();
            case ALL -> this.residueGraphOptions = ResidueGraph.ResidueGraphOptions.all();
            default -> throw new IllegalArgumentException("Don't know how to handle " + strucmotifConfig.getResidueGraphStrategy());
        }
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
            if (!dirtyStructureIdentifiers.isEmpty()) {
                logger.warn("Update state is dirty - Problematic identifiers:\n{}",
                        dirtyStructureIdentifiers);
                logger.info("Recovering from dirty state");
                recover(dirtyStructureIdentifiers);
            }
        }

        // ensure no partial files linger
        deletePartialFiles();

        logger.info("Starting update - Operation: {}, {} ids ({})",
                operation,
                requested.size(),
                requested.stream()
                        .limit(5)
                        .map(item -> "\"" + item.getStructureIdentifier() + "\"")
                        .collect(Collectors.joining(", ", "[",  requested.size() > 5 ? ", ...]" : "]")));

        switch (operation) {
            case ADD -> add(new Context(strucmotifConfig, getDeltaPlusIdentifiers(requested)));
            case REMOVE -> remove(getDeltaMinusIdentifiers(requested));
            case RECOVER -> recover(stateRepository.selectDirty());
        }

        // ensure no partial files files
        deletePartialFiles();

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
     * @throws IOException writing to index fails
     */
    public void add(Context context) throws IOException {
        long target = context.updateItems.size();
        logger.info("{} files to process in total", target);

        Partition<UpdateItem> partitions = new Partition<>(context.updateItems, strucmotifConfig.getCommitInterval());
        if (partitions.size() > 1) {
            logger.info("Formed {} partitions of {} structures", partitions.size(), strucmotifConfig.getCommitInterval());
        }

        Set<String> known = getKnown();
        boolean needsCommit = false;

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            List<UpdateItem> partition = partitions.get(i);
            context.partitionSize = partition.size();
            context.partitionContext = (i + 1) + " / " + partitions.size();
            context.structureCounter = new AtomicInteger();
            partition.parallelStream().forEach(item -> handleUpdateItem(item, context));
            needsCommit = true;

            if (context.structureCounter.get() > 0) {
                commit(context, known);
                needsCommit = false;
            }
        }

        if (needsCommit) {
            commit(context, known);
        }
    }

    private void commit(Context context, Set<String> known) throws IOException {
        logger.info("Committing data on {} structures", context.processed.size());
        // flush per-thread streams
        context.flush();

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
        context.close();
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
                String structureContext = count + " / " + context.partitionSize + "] [" + source;
                logger.warn("[{}] [{}] [try: {} / {}] Failed to download or parse source file - {}",
                        context.partitionContext, structureContext, i, maxRetries, e.getMessage());
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
            int majorRevision = 1;
            int minorRevision = 0;
            if (revisionHistory.isDefined()) {
                majorRevision = revisionHistory.getMajorRevision().get(revisionHistory.getRowCount() - 1);
                minorRevision = revisionHistory.getMinorRevision().get(revisionHistory.getRowCount() - 1);
            } else {
                switch (strucmotifConfig.getMissingRevisionStrategy()) {
                    case IGNORE -> {}
                    case WARN -> logger.warn("[{}] [{}] 'pdbx_audit_revision_history' is undefined, defaulting to 1.0", context.partitionContext, structureIdentifier);
                    case FAIL -> throw new IllegalArgumentException("'pdbx_audit_revision_history' is mandatory in input files - rejecting " + structureIdentifier);
                }
            }
            PdbxStructAssembly pdbxStructAssembly = block.getPdbxStructAssembly();
            PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
            PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
            if (!pdbxStructAssembly.isDefined() || !pdbxStructAssemblyGen.isDefined() || !pdbxStructOperList.isDefined()) {
                switch (strucmotifConfig.getMissingAssemblyStrategy()) {
                    case IGNORE -> {}
                    case WARN -> logger.warn("[{}] [{}] 'pdbx_struct_assembly', 'pdbx_struct_assembly_gen', or 'pdbx_struct_oper_list' are undefined", context.partitionContext, structureIdentifier);
                    case FAIL -> throw new IllegalArgumentException("'pdbx_struct_assembly', 'pdbx_struct_assembly_gen', and 'pdbx_struct_oper_list' are mandatory in input files - rejecting " + structureIdentifier);
                }
            }

            // write renumbered structure
            logger.debug("[{}] [{}] Writing renumbered structure file", context.partitionContext, structureIdentifier);
            structureDataProvider.writeRenumbered(structureIdentifier, mmCifFile, item.getModelIdentifier());
            context.processed.add(new StructureInformation(structureIdentifier, structureIndex, majorRevision, minorRevision));
        } catch (IOException e) {
            throw new UncheckedIOException("Cif parsing failed for " + structureIdentifier, e);
        } catch (ParsingException e) {
            throw new ParsingException("Cif parsing failed for " + structureIdentifier, e);
        }

        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + context.partitionSize + "] [" + structureIdentifier;

        // fails when file is missing, expected when e.g. all residues of the file where below the pLDDT threshold
        Structure structure;
        try {
            structure = structureDataProvider.readRenumbered(structureIdentifier);
        } catch (UncheckedIOException e) {
            logger.warn("[{}] [{}] No renumbered source file present - Skipping", context.partitionContext, structureContext);
            return;
        }

        try {
            long startGraph = System.nanoTime();
            ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, residueGraphOptions);
            logger.info("[{}] [{}] Computed residue graph ({} residues, {} pairs) in {} ms",
                    context.partitionContext,
                    structureContext,
                    residueGraph.getResidueCount(),
                    residueGraph.getPairingCount(),
                    (System.nanoTime() - startGraph) / 1000 / 1000);

            // extract motifs, sort into bins of same prefix
            long startWrite = System.nanoTime();
            int magicStructureIndex = structureIndex | 1 << 31;
            byte[] buffer = new byte[8];
            AtomicInteger structureMotifCounter = new AtomicInteger();
            AtomicInteger lastDescriptor = new AtomicInteger(); // 0 is safe as nothing should be AA-0-0-0
            residueGraph.residuePairOccurrencesSequential()
                    .sorted(Comparator.comparingInt(ResiduePairOccurrence::getResiduePairDescriptor))
                    .forEach(o -> {
                        try {
                            int descriptor = o.getResiduePairDescriptor();
                            OutputStream outputStream = context.getOutputStream(descriptor);
                            if (descriptor != lastDescriptor.get()) {
                                buffer[0] = (byte) (magicStructureIndex >>> 24);
                                buffer[1] = (byte) (magicStructureIndex >>> 16);
                                buffer[2] = (byte) (magicStructureIndex >>> 8);
                                buffer[3] = (byte) magicStructureIndex;
                                buffer[4] = (byte) (descriptor >>> 24);
                                buffer[5] = (byte) (descriptor >>> 16);
                                buffer[6] = (byte) (descriptor >>> 8);
                                buffer[7] = (byte) descriptor;
                                outputStream.write(buffer);
                                lastDescriptor.set(descriptor);
                            }
                            long identifier = o.getResiduePairIdentifier();
                            buffer[0] = (byte) (identifier >>> 56);
                            buffer[1] = (byte) (identifier >>> 48);
                            buffer[2] = (byte) (identifier >>> 40);
                            buffer[3] = (byte) (identifier >>> 32);
                            buffer[4] = (byte) (identifier >>> 24);
                            buffer[5] = (byte) (identifier >>> 16);
                            buffer[6] = (byte) (identifier >>> 8);
                            buffer[7] = (byte) identifier;
                            outputStream.write(buffer);
                            structureMotifCounter.incrementAndGet();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            logger.info("[{}] [{}] Wrote {} residue pairs in {} ms",
                    context.partitionContext,
                    structureContext,
                    structureMotifCounter.get(),
                    (System.nanoTime() - startWrite) / 1000 / 1000);
        } catch (Exception e) {
            logger.error("[{}] [{}] Residue graph determination failed",
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
            logger.info("[{}] Processing {}", item.getStructureIdentifier(), url);
            return url.openStream();
        }

        return structureDataProvider.getOriginalInputStream(item.getStructureIdentifier());
    }

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
                invertedIndex.delete(mapped);
            }
        }

        logger.info("Updating holdings");
        stateRepository.deleteKnown(identifiers);
        stateRepository.deleteDirty(identifiers);
        logger.info("Finished removal operation");
    }

    /**
     * Reports all structures currently present in the PDB archive.
     * @param mode what data (pdb/csm) to include
     * @return collection of update items
     * @throws IOException connection failure
     */
    public List<UpdateItem> getAllIdentifiers(String mode) throws IOException {
        logger.info("Retrieving current {} entry list from RCSB PDB Search API", mode);
        URL url = composeSearchUrl(mode);
        logger.info("URL: {}", url);
        List<UpdateItem> out = new ArrayList<>();
        try (InputStream inputStream = url.openStream()) {
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

        String query = URLEncoder.encode("""
                {
                  "query": {
                    "type": "terminal",
                    "label": "text",
                    "service": "text",
                    "parameters": {
                      "attribute": "rcsb_entry_container_identifiers.entry_id",
                      "operator": "exists",
                      "negation": false
                    }
                  },
                  "return_type": "entry",
                  "request_options": {
                    "results_content_type": [{types}],
                    "return_all_hits": true,
                    "results_verbosity": "compact"
                  }
                }
                """.replace("{types}", types), StandardCharsets.UTF_8);
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
                    .toList();
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
                    .toList();
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

    private static final String PDB_REGEX = "^[1-9][a-zA-Z0-9]{3}|PDB_[a-zA-Z0-9]{8}$";
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
                        // can't be toList() as shuffle will happen downstream
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
                            throw new IllegalArgumentException("Cannot parse line: '" + id + "' - format is '${4-character-entryId}' or '${identifier},${url}'");
                        }
                    })
                    // can't be toList() as shuffle will happen downstream
                    .collect(Collectors.toList());
        }
        Collections.shuffle(requested);

        return requested;
    }

    private void deletePartialFiles() throws IOException {
        try (Stream<Path> paths = Files.list(Paths.get(strucmotifConfig.getRootPath()))) {
            paths.filter(p -> p.getFileName().toString().startsWith(StrucmotifConfig.INDEX) && p.getFileName().endsWith(StrucmotifConfig.TMP_EXT))
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
