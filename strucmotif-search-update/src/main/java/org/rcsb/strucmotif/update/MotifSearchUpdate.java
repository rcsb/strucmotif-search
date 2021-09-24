package org.rcsb.strucmotif.update;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.ParsingException;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Revision;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.math.Partition;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Runs strucmotif updates from the command-line.
 */
@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@ComponentScan({"org.rcsb.strucmotif"})
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchUpdate implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);

    /**
     * Entry point from the command-line.
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MotifSearchUpdate.class, args);
    }

    private final StateRepository stateRepository;
    private final StructureDataProvider structureDataProvider;
    private final InvertedIndex invertedIndex;
    private final MotifSearchConfig motifSearchConfig;
    private final ThreadPool threadPool;

    /**
     * Injectable constructor.
     * @param stateRepository the state repo
     * @param structureDataProvider data provider
     * @param invertedIndex inverted index
     * @param motifSearchConfig configs
     * @param threadPool thread pool
     */
    @Autowired
    public MotifSearchUpdate(StateRepository stateRepository, StructureDataProvider structureDataProvider, InvertedIndex invertedIndex, MotifSearchConfig motifSearchConfig, ThreadPool threadPool) {
        this.stateRepository = stateRepository;
        this.structureDataProvider = structureDataProvider;
        this.invertedIndex = invertedIndex;
        this.motifSearchConfig = motifSearchConfig;
        this.threadPool = threadPool;
    }

    /**
     * Actual run method with the given arguments.
     * @param args command-line arguments
     * @throws Exception update failure
     */
    public void run(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Too few arguments");
            System.out.println("Usage: java -Xmx12G -jar update.jar operation ...");
            System.out.println("Valid operation values: " + Arrays.toString(Operation.values()));
            System.out.println("Optionally: list of entry ids - (no argument performs null operation, use single argument 'full' for complete update)");
            System.out.println("If you want to update entries you have to explicitly remove them first");
            System.out.println("Example: java -Xmx12G -jar update.jar ADD 1acj 1exr 4hhb");
            return;
        }

        // determine identifiers requested by user - either provided collection or all currently reported identifiers by RCSB PDB
        Operation operation = Operation.resolve(args[0]);
        String[] ids = new String[args.length - 1];
        List<String> requested;
        System.arraycopy(args, 1, ids, 0, ids.length);
        if (ids.length == 1 && ids[0].equalsIgnoreCase("full")) {
            requested = getAllIdentifiers();
        } else {
            requested = Arrays.stream(ids).map(String::toUpperCase).collect(Collectors.toList());
        }
        Collections.shuffle(requested);

        // check for sanity of internal state
        if (operation != Operation.RECOVER) {
            Collection<String> dirtyStructureIdentifiers = stateRepository.selectDirty();
            if (dirtyStructureIdentifiers.size() > 0) {
                logger.warn("Update state is dirty - problematic identifiers:\n{}",
                        dirtyStructureIdentifiers);
                logger.info("Recovering from dirty state");
                remove(stateRepository.selectDirty());
            }
        }

        logger.info("Starting update - Operation: {}, {} ids ({})",
                operation,
                requested.size(),
                requested.stream()
                        .limit(5)
                        .map(id -> "\"" + id + "\"")
                        .collect(Collectors.joining(", ",
                                "[",
                                requested.size() > 5 ? ", ...]" : "]")));

        switch (operation) {
            case ADD:
                add(getDeltaPlusIdentifiers(requested));
                break;
            case REMOVE:
                remove(getDeltaMinusIdentifiers(requested));
                break;
            case RECOVER:
                remove(stateRepository.selectDirty());
                break;
        }

        logger.info("Finished update operation");
    }

    /**
     * The 'ADD' operation.
     * @param identifiers set of identifiers to add
     * @throws ExecutionException update failure
     * @throws InterruptedException update failure
     */
    public void add(Collection<String> identifiers) throws ExecutionException, InterruptedException {
        long target = identifiers.size();
        logger.info("{} files to process in total", target);

        Partition<String> partitions = new Partition<>(identifiers, motifSearchConfig.getUpdateChunkSize());
        logger.info("Formed {} partitions of {} structures",
                partitions.size(),
                motifSearchConfig.getUpdateChunkSize());

        Context context = new Context();

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            context.partitionContext = (i + 1) + " / " + partitions.size();

            List<String> partition = partitions.get(i);
            logger.info("[{}] Start processing partition", context.partitionContext);

            context.structureCounter = new AtomicInteger();
            context.buffer = new ConcurrentHashMap<>();
            threadPool.submit(() -> {
                partition.parallelStream().forEach(id -> handleStructureIdentifier(id, context));
                return null;
            }).get();

            // mark as dirty only around index update
            stateRepository.insertDirty(partition);
            persist(context);
        }
    }

    static class Context {
        final Set<StructureInformation> processed;
        String partitionContext;
        Map<ResiduePairDescriptor, Map<String, Collection<ResiduePairIdentifier>>> buffer;
        AtomicInteger structureCounter;

        public Context() {
            this.processed = Collections.synchronizedSet(new HashSet<>());
        }
    }

    private void handleStructureIdentifier(String structureIdentifier, Context context) {
        int maxRetries = motifSearchConfig.getDownloadTries();
        for (int i = 1; i <= maxRetries; i++) {
            try {
                handleStructureIdentifierInternal(structureIdentifier, context);
                break;
            } catch (UncheckedIOException e) {
                if (i >= maxRetries) {
                    // max retries exceeded
                    throw e;
                }

                int count = context.structureCounter.get();
                String structureContext = count + " / " + motifSearchConfig.getUpdateChunkSize() + "] [" + structureIdentifier;
                logger.warn("[{}] [{}] [try: {} / {}] Failed to download source file - {}",
                        context.partitionContext,
                        structureContext,
                        i,
                        maxRetries,
                        e.getMessage());
            }
        }
    }

    private void handleStructureIdentifierInternal(String structureIdentifier, Context context) {
        try {
            // write renumbered structure
            MmCifFile mmCifFile = CifIO.readFromInputStream(structureDataProvider.getOriginalInputStream(structureIdentifier)).as(StandardSchemata.MMCIF);
            Revision revision = new Revision(mmCifFile);
            Map<String, Set<String>> assemblyInformation = getAssemblyInformation(mmCifFile);
            structureDataProvider.writeRenumbered(structureIdentifier, mmCifFile);
            context.processed.add(new StructureInformation(structureIdentifier, revision, assemblyInformation));
        } catch (IOException e) {
            throw new UncheckedIOException("cif parsing failed for " + structureIdentifier, e);
        } catch (ParsingException e) {
            logger.info("cif parsing failed for " + structureIdentifier, e);
            throw e;
        }

        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + motifSearchConfig.getUpdateChunkSize() + "] [" + structureIdentifier;

        // fails when file is missing (should not happen) or does not contain valid polymer chain
        Structure structure;
        try {
            structure = structureDataProvider.readRenumbered(structureIdentifier);
        } catch (UncheckedIOException e) {
            logger.warn("[{}] [{}] No valid polymer chains",
                    context.partitionContext,
                    structureContext);
            return;
        }

        try {
            ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff(), false);

            // extract motifs
            AtomicInteger structureMotifCounter = new AtomicInteger();
            threadPool.submit(() -> {
                residueGraph.residuePairOccurrencesParallel()
                        .forEach(motifOccurrence -> {
                            ResiduePairDescriptor motifDescriptor = motifOccurrence.getResiduePairDescriptor();
                            ResiduePairIdentifier targetIdentifier = motifOccurrence.getResidueIdentifier();

                            Map<String, Collection<ResiduePairIdentifier>> groupedTargetIdentifiers = context.buffer.computeIfAbsent(motifDescriptor, k -> Collections.synchronizedMap(new HashMap<>()));
                            Collection<ResiduePairIdentifier> targetIdentifiers = groupedTargetIdentifiers.computeIfAbsent(structureIdentifier, k -> Collections.synchronizedSet(new HashSet<>()));
                            targetIdentifiers.add(targetIdentifier);
                            structureMotifCounter.incrementAndGet();
                        });
                return null;
            }).get();
            logger.info("[{}] [{}] Extracted {} residue pairs",
                    context.partitionContext,
                    structureContext,
                    structureMotifCounter.get());
        } catch (Exception e) {
            logger.warn("[{}] [{}] Residue graph determination failed",
                    context.partitionContext,
                    structureContext,
                    e);
            // fail complete update
            throw new RuntimeException(e);
        }
    }

    private Map<String, Set<String>> getAssemblyInformation(MmCifFile mmCifFile) {
        // TODO maybe this functionality should be part of Structures?
        /*
        loop_
        _pdbx_struct_assembly_gen.assembly_id
        _pdbx_struct_assembly_gen.oper_expression
        _pdbx_struct_assembly_gen.asym_id_list
        1 '(1-60)(61-88)'           A,B,C
        2 '(61-88)'                 A,B,C
        3 '(1-5)(61-88)'            A,B,C
        4 '(1,2,6,10,23,24)(61-88)' A,B,C
        5 '(1-5)(63-68)'            A,B,C
        6 '(1,10,23)(61,62,69-88)'  A,B,C
        7 '(P)(61-88)'              A,B,C
        #
         */
        PdbxStructAssemblyGen pdbxStructAssemblyGen = mmCifFile.getFirstBlock().getPdbxStructAssemblyGen();
        Map<String, Set<String>> assemblyInformation = new LinkedHashMap<>();
        if (pdbxStructAssemblyGen.isDefined()) {
            for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
                String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
                String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
                String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
                List<String> operList = getOperList(operExpression, asymIdList);

                Set<String> sorted = assemblyInformation.computeIfAbsent(assemblyId, e -> new HashSet<>());
                sorted.addAll(operList);
            }
        }
        return assemblyInformation;
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private static final Pattern LIST_PATTERN = Pattern.compile(",");
    private List<String> getOperList(String operExpression, String asymIdList) {
        List<String> operations = new ArrayList<>();
        List<String> chains = LIST_PATTERN.splitAsStream(asymIdList).collect(Collectors.toList());
        String[] split = OPERATION_PATTERN.split(operExpression);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    for (String chain : chains) {
                        operations.add(chain + "_" + id1 + "x" + id2);
                    }
                }
            }
        } else {
            for (String id : extractTransformationIds(operExpression)) {
                for (String chain : chains) {
                    operations.add(chain + "_" + id);
                }
            }
        }

        return operations;
    }

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private List<String> extractTransformationIds(String rawOperation) {
        String prepared = rawOperation.replace("(", "")
                .replace(")", "")
                .replace("'", "");

        return COMMA_PATTERN.splitAsStream(prepared)
                .flatMap(this::extractTransformationRanges)
                .collect(Collectors.toList());
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("-");
    private Stream<String> extractTransformationRanges(String raw) {
        String[] s = RANGE_PATTERN.split(raw);
        if (s.length == 1) {
            return Stream.of(raw);
        } else {
            return IntStream.range(Integer.parseInt(s[0]), Integer.parseInt(s[1]) + 1)
                    .mapToObj(String::valueOf);
        }
    }

    private void persist(Context context) throws ExecutionException, InterruptedException {
        logger.info("[{}] Persisting {} unique residue pair descriptors",
                context.partitionContext,
                context.buffer.size());

        final int bufferTotal = context.buffer.size();
        AtomicInteger bufferCount = new AtomicInteger();
        threadPool.submit(() -> {
            context.buffer.entrySet().parallelStream().forEach(entry -> {
                ResiduePairDescriptor full = entry.getKey();
                Map<String, Collection<ResiduePairIdentifier>> output = entry.getValue();

                if (bufferCount.incrementAndGet() % 100000 == 0) {
                    logger.info("[{}] {} / {}",
                            context.partitionContext,
                            bufferCount,
                            bufferTotal);
                }

                invertedIndex.insert(full, output);

                // writing takes additional heap - ease burden by dropping processed output bins
                output.clear();
            });
            return null;
        }).get();

        context.buffer.clear();

        // processed contains all StructureIdentifiers + corresponding revision
        stateRepository.insertKnown(context.processed);
        stateRepository.deleteDirty(context.processed.stream().map(StructureInformation::getStructureIdentifier).collect(Collectors.toSet()));
        context.processed.clear();
    }

    /**
     * 'REMOVE' operation.
     * @param identifiers set of identifiers to remove
     */
    public void remove(Collection<String> identifiers) {
        AtomicInteger counter = new AtomicInteger();
        for (String structureIdentifier : identifiers) {
            logger.info("[{}] Removing renumbered structure for entry: {}",
                    counter.incrementAndGet() + " / " + identifiers.size(),
                    structureIdentifier);
            structureDataProvider.deleteRenumbered(structureIdentifier);
        }

        // inverted index is expensive and should be done as batch
        if (identifiers.size() > 0) {
            invertedIndex.delete(identifiers);
            stateRepository.deleteKnown(identifiers);
            stateRepository.deleteDirty(identifiers);
        }
        logger.info("Finished removal operation");
    }

    private static final Pattern ENTRY_ID_PATTERN = Pattern.compile("[0-9][0-9A-Z]{3}");

    /**
     * Reports all structures currently present in the PDB archive.
     * @return collection of structure identifiers
     * @throws IOException connection failure
     */
    public List<String> getAllIdentifiers() throws IOException {
        logger.info("Retrieving current entry list from {}", MotifSearchConfig.RCSB_ENTRY_LIST);
        String response;
        try (InputStream inputStream = new URL(MotifSearchConfig.RCSB_ENTRY_LIST).openStream()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                response = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
        return ENTRY_ID_PATTERN.matcher(response)
                .results()
                .map(MatchResult::group)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @return array of IDs that need to be processed for the given context
     */
    public Collection<String> getDeltaPlusIdentifiers(Collection<String> requested) {
        Collection<String> known = stateRepository.selectKnown().stream().map(StructureInformation::getStructureIdentifier).collect(Collectors.toSet());
        if (known.isEmpty()) {
            logger.warn("No existing data - starting from scratch");
            return requested;
        } else {
            return requested.stream()
                    .filter(id -> !known.contains(id))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param requested the requested update
     * @return array of IDs that need to be remove for the given context
     */
    public Collection<String> getDeltaMinusIdentifiers(Collection<String> requested) {
        Collection<String> known = stateRepository.selectKnown().stream().map(StructureInformation::getStructureIdentifier).collect(Collectors.toSet());
        if (known.isEmpty()) {
            logger.warn("No existing data - no need for cleanup of obsolete entries");
            return Collections.emptySet();
        } else {
            return known.stream()
                    .filter(requested::contains)
                    .collect(Collectors.toSet());
        }
    }
}
