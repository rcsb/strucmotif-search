package org.rcsb.strucmotif.update;

import com.google.gson.Gson;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.ResidueGraph;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.StateRepository;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootApplication(exclude = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
@ComponentScan({"org.rcsb.strucmotif"})
@EntityScan("org.rcsb.strucmotif")
public class MotifSearchUpdate implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);
    private static final String RCSB_ENTRY_LIST = "http://www.rcsb.org/pdb/json/getCurrent";

    public static void main(String[] args) {
        SpringApplication.run(MotifSearchUpdate.class, args);
    }

    private final StateRepository stateRepository;
    private final StructureDataProvider structureDataProvider;
    private final InvertedIndex invertedIndex;
    private final MotifSearchConfig motifSearchConfig;

    @Autowired
    public MotifSearchUpdate(StateRepository stateRepository, StructureDataProvider structureDataProvider, InvertedIndex invertedIndex, MotifSearchConfig motifSearchConfig) {
        this.stateRepository = stateRepository;
        this.structureDataProvider = structureDataProvider;
        this.invertedIndex = invertedIndex;
        this.motifSearchConfig = motifSearchConfig;
    }

    public void run(String[] args) throws IOException {
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
        List<StructureIdentifier> requested;
        System.arraycopy(args, 1, ids, 0, ids.length);
        if (ids.length == 1 && ids[0].equalsIgnoreCase("full")) {
            requested = getAllIdentifiers();
        } else {
            requested = Arrays.stream(ids).map(StructureIdentifier::new).collect(Collectors.toList());
        }
        // shuffle to prevent troublemakers such as ribosome and virus capsids occurring in the same chunk
        Collections.shuffle(requested);

        // check for sanity of internal state
        if (operation != Operation.RECOVER) {
            Collection<StructureIdentifier> dirtyStructureIdentifiers = stateRepository.selectDirty();
            if (dirtyStructureIdentifiers.size() > 0) {
                logger.warn("Update state is dirty - problematic identifiers:\n{}",
                        dirtyStructureIdentifiers);
                logger.warn("This requires manual intervention - perform 'RECOVER' operation and rerun update");
                throw new IllegalStateException("Update state is dirty - problematic identifiers:\n" + dirtyStructureIdentifiers);
            }
        }

        logger.info("Starting update - Operation: {}, {} ids ({})",
                operation,
                requested.size(),
                requested.stream()
                        .limit(5)
                        .map(id -> "\"" + id.getPdbId() + "\"")
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

    private void add(Collection<StructureIdentifier> identifiers) {
        long target = identifiers.size();
        logger.info("{} files to process in total", target);

        Partition<StructureIdentifier> partitions = new Partition<>(identifiers, motifSearchConfig.getUpdateChunkSize());
        logger.info("Formed {} partitions of {} structures",
                partitions.size(),
                motifSearchConfig.getUpdateChunkSize());

        Context context = new Context();

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            context.partitionContext = (i + 1) + " / " + partitions.size();

            List<StructureIdentifier> partition = partitions.get(i);
            logger.info("[{}] Start processing partition", context.partitionContext);
            stateRepository.insertDirty(partition);

            context.structureCounter = new AtomicInteger();
            context.buffer = new ConcurrentHashMap<>();
            partition.parallelStream().forEach(id -> handleStructureIdentifier(id, context));

            persist(partition, context);
        }
    }

    static class Context {
        final Set<StructureIdentifier> processed;
        String partitionContext;
        Map<ResiduePairDescriptor, Map<StructureIdentifier, Collection<ResiduePairIdentifier>>> buffer;
        AtomicInteger structureCounter;

        public Context() {
            this.processed = Collections.synchronizedSet(new HashSet<>());
        }
    }

    private void handleStructureIdentifier(StructureIdentifier structureIdentifier, Context context) {
        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + motifSearchConfig.getUpdateChunkSize() + "] [" + structureIdentifier.getPdbId();

        try {
            // write renumbered structure
            MmCifFile mmCifFile = CifIO.readFromInputStream(structureDataProvider.getOriginalInputStream(structureIdentifier)).as(StandardSchemata.MMCIF);
            structureDataProvider.writeRenumbered(structureIdentifier, mmCifFile);
        } catch (IOException e) {
            throw new UncheckedIOException("cif parsing failed for " + structureIdentifier, e);
        }

        // fails when file is missing (should not happen) or does not contain valid polymer chain
        Structure structure;
        try {
            structure = structureDataProvider.readRenumbered(structureIdentifier);
        } catch (UncheckedIOException e) {
            // can 'safely' happen when obsolete entry was dropped from bcif data but still lingers in list
            logger.warn("[{}] [{}] Source file missing unexpectedly - obsolete entry?",
                    context.partitionContext,
                    structureContext,
                    e);
            return;
        } catch (UnsupportedOperationException e) {
            logger.warn("[{}] [{}] No valid polymer chains",
                    context.partitionContext,
                    structureContext);
            return;
        }

        try {
            ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

            // extract motifs
            AtomicInteger structureMotifCounter = new AtomicInteger();
            residueGraph.residuePairOccurrencesParallel()
                    .forEach(motifOccurrence -> {
                        ResiduePairDescriptor motifDescriptor = motifOccurrence.getResiduePairDescriptor();
                        ResiduePairIdentifier targetIdentifier = motifOccurrence.getResidueIdentifier();

                        Map<StructureIdentifier, Collection<ResiduePairIdentifier>> groupedTargetIdentifiers = context.buffer.computeIfAbsent(motifDescriptor, k -> Collections.synchronizedMap(new HashMap<>()));
                        Collection<ResiduePairIdentifier> targetIdentifiers = groupedTargetIdentifiers.computeIfAbsent(structureIdentifier, k -> Collections.synchronizedSet(new HashSet<>()));
                        targetIdentifiers.add(targetIdentifier);
                        structureMotifCounter.incrementAndGet();
                    });
            logger.info("[{}] [{}] Extracted {} residue pairs",
                    context.partitionContext,
                    structureContext,
                    structureMotifCounter.get());
            context.processed.add(structureIdentifier);
        } catch (Exception e) {
            logger.warn("[{}] [{}] Residue graph determination failed",
                    context.partitionContext,
                    structureContext,
                    e);
            // fail complete update
            throw e;
        }
    }

    private void persist(Collection<StructureIdentifier> requested, Context context) {
        logger.info("[{}] Persisting {} unique residue pair descriptors",
                context.partitionContext,
                context.buffer.size());

        final int bufferTotal = context.buffer.size();
        AtomicInteger bufferCount = new AtomicInteger();
        context.buffer.entrySet().parallelStream().forEach(entry -> {
            ResiduePairDescriptor full = entry.getKey();
            Map<StructureIdentifier, Collection<ResiduePairIdentifier>> output = entry.getValue();

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
        context.buffer.clear();

        // requested is the user specified collection
        stateRepository.insertKnown(requested);
        // processed are those that resemble valid additions to the search space
        stateRepository.insertSupported(context.processed);
        stateRepository.deleteDirty(requested);
        context.processed.clear();
    }

    private void remove(Collection<StructureIdentifier> identifiers) {
        for (StructureIdentifier structureIdentifier : identifiers) {
            logger.info("Removing renumbered structure for entry: {}", structureIdentifier);
            structureDataProvider.deleteRenumbered(structureIdentifier);

            // update state for known & supported
            Set<StructureIdentifier> update = Set.of(structureIdentifier);
            stateRepository.deleteKnown(update);
            stateRepository.deleteSupported(update);
        }

        // inverted index is expensive and should be done as batch
        if (identifiers.size() > 0) {
            invertedIndex.delete(identifiers);
            stateRepository.deleteDirty(identifiers);
        }
        logger.info("Finished removal operation");
    }

    public List<StructureIdentifier> getAllIdentifiers() throws IOException {
        logger.info("Retrieving current entry list from {}", RCSB_ENTRY_LIST);
        GetCurrentResponse response;
        try (InputStream inputStream = new URL(RCSB_ENTRY_LIST).openStream()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                response = new Gson().fromJson(inputStreamReader, GetCurrentResponse.class);
            }
        }
        return Arrays.stream(response.getIdList())
                .map(String::toLowerCase)
                .map(StructureIdentifier::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    static class GetCurrentResponse {
        private int resultCount;
        private String[] idList;

        int getResultCount() {
            return resultCount;
        }

        void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        String[] getIdList() {
            return idList;
        }

        void setIdList(String[] idList) {
            this.idList = idList;
        }
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @return array of IDs that need to be processed for the given context
     */
    public Collection<StructureIdentifier> getDeltaPlusIdentifiers(Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = stateRepository.selectKnown();
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
    public Collection<StructureIdentifier> getDeltaMinusIdentifiers(Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = stateRepository.selectKnown();
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
