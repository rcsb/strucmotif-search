package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.ResidueGraph;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.nio.file.Path;
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
import java.util.stream.Collectors;

/**
 * Update the inverted index.
 */
@Service
public class AddStructuresToInvertedIndexTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToInvertedIndexTask.class);
    private final InvertedIndex motifLookup;
    private final RenumberedStructureReader renumberedReader;
    private final MotifSearchConfig motifSearchConfig;
    private final StateRepository stateRepository;

    @Autowired
    public AddStructuresToInvertedIndexTask(InvertedIndex motifLookup, RenumberedStructureReader renumberedReader, MotifSearchConfig motifSearchConfig, StateRepository stateRepository) {
        this.motifLookup = motifLookup;
        this.renumberedReader = renumberedReader;
        this.motifSearchConfig = motifSearchConfig;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting structural motif search inverted index update");

        List<StructureIdentifier> identifiers = new ArrayList<>(delta);
        // we shuffle because certain 'troublemakers' (e.g. ribosomes or virus capsids) appear close together, in a full update this leads to 1 bin maxing out available heap space
        Collections.shuffle(identifiers);

        // we assume that the argument list does not contain any identifiers already present in the index
        // work on optimized path so that component index mapping is valid
        List<Path> paths = identifiers.stream()
                .map(id -> motifSearchConfig.getOriginalStructurePath().resolve(id + ".bcif.gz"))
                .collect(Collectors.toList());

        long totalFileCount = paths.size();
        logger.info("{} files to process in total", totalFileCount);

        Partition<Path> partitions = new Partition<>(paths, motifSearchConfig.getChunkSize());
        logger.info("Formed {} partitions", partitions.size());

        Context context = new Context(stateRepository);

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            context.partitionContext = (i + 1) + " / " + partitions.size();
            logger.info("[{}] Start processing partition", context.partitionContext);

            context.structureCounter = new AtomicInteger();
            context.buffer = new ConcurrentHashMap<>();
            partitions.get(i)
                    .parallelStream()
                    .forEach(path -> detectMotifs(path, context));

            writeMotifs(context);
        }

        logger.info("Finished inverted index update");
    }

    static class Context {
        final StateRepository updateStateManager;
        final Set<StructureIdentifier> processed;
        String partitionContext;
        Map<ResiduePairDescriptor, Map<StructureIdentifier, Collection<ResiduePairIdentifier>>> buffer;
        AtomicInteger structureCounter;

        public Context(StateRepository updateStateManager) {
            this.updateStateManager = updateStateManager;
            this.processed = Collections.synchronizedSet(new HashSet<>());
        }
    }

    private void detectMotifs(Path path, Context context) {
        String pdbId = path.toFile().getName().split("\\.")[0];
        StructureIdentifier structureIdentifier = new StructureIdentifier(pdbId);
        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + motifSearchConfig.getChunkSize() + "] [" + pdbId;

        // fails when file is missing (should not happen) or does not contain valid polymer chain
        Structure structure;
        try {
            structure = renumberedReader.readById(structureIdentifier);
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

    private void writeMotifs(Context context) {
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

            motifLookup.insert(full, output);

            // writing takes additional heap - ease burden by dropping processed output bins
            output.clear();
        });
        context.buffer.clear();

        context.updateStateManager.insertIndexed(context.processed);
        context.processed.clear();
    }
}
