package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.IndexSelectionResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * The default strucmotif-search target assembler.
 */
@Service
public class TargetAssemblerImpl implements TargetAssembler {
    private static final Logger logger = LoggerFactory.getLogger(TargetAssemblerImpl.class);
    private final InvertedIndex invertedIndex;
    private final ThreadPool threadPool;
    private final StructureIndexProvider structureIndexProvider;

    /**
     * Injectable constructor.
     * @param invertedIndex inverted index
     * @param threadPool thread pool
     * @param structureIndexProvider maps from structureIdentifiers to indices
     */
    @Autowired
    public TargetAssemblerImpl(InvertedIndex invertedIndex, ThreadPool threadPool, StructureIndexProvider structureIndexProvider) {
        this.invertedIndex = invertedIndex;
        this.threadPool = threadPool;
        this.structureIndexProvider = structureIndexProvider;
    }

    @Override
    public void assemble(MotifSearchResult response) throws ExecutionException, InterruptedException {
        MotifSearchQuery query = response.getQuery();
        QueryStructure queryStructure = query.getQueryStructure();
        Parameters parameters = query.getParameters();
        int backboneDistanceTolerance = parameters.getBackboneDistanceTolerance();
        int sideChainDistanceTolerance = parameters.getSideChainDistanceTolerance();
        int angleTolerance = parameters.getAngleTolerance();
        Map<LabelSelection, Set<ResidueType>> labelSelectionExchanges = query.getExchanges();
        Map<IndexSelection, Set<ResidueType>> exchanges = labelSelectionExchanges.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> {
                    LabelSelection labelSelection = entry.getKey();
                    int residueIndex = queryStructure.getStructure().getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId());
                    return new IndexSelection(labelSelection.getStructOperId(), residueIndex);
                }, Map.Entry::getValue));
        Set<Integer> allowed = query.getWhitelist()
                .stream()
                .map(structureIndexProvider::selectStructureIndex)
                .collect(Collectors.toSet());
        Set<Integer> ignored = query.getBlacklist()
                .stream()
                .map(structureIndexProvider::selectStructureIndex)
                .collect(Collectors.toSet());

        response.getTimings().pathsStart();
        // retrieve target identifiers per query motif descriptor
        for (ResiduePairOccurrence residuePairOccurrence : queryStructure.getResiduePairOccurrences()) {
            long s = System.nanoTime();
            ResiduePairDescriptor residuePairDescriptor = residuePairOccurrence.getResiduePairDescriptor();

            // sort into target structures
            threadPool.submit(() -> {
                residuePairOccurrence.residuePairDescriptorsByTolerance(backboneDistanceTolerance, sideChainDistanceTolerance, angleTolerance, exchanges)
                        // create views on inverted index data
                        .map(invertedIndex::select)
                        .forEach(bucket -> consume(response, bucket, allowed, ignored));
                return null;
            }).get();

            logger.info("[{}] Consumed {} in {} ms - {} valid target structures remaining",
                    response.getQuery().hashCode(),
                    residuePairDescriptor,
                    (System.nanoTime() - s) / 1000 / 1000,
                    response.getTargetStructures().size());
        }
        response.getTimings().pathsStop();

        int pathCount = response.getTargetStructures().values().stream().mapToInt(TargetStructure::getNumberOfValidPaths).sum();
        int structureCount = response.getTargetStructures().size();
        logger.info("[{}] Found {} valid paths ({} target structures) in {} ms",
                response.getQuery().hashCode(),
                pathCount,
                structureCount,
                response.getTimings().getPathsTime());
        response.setNumberOfPaths(pathCount);
        response.setNumberOfTargetStructures(structureCount);
    }

    private void consume(MotifSearchResult response, InvertedIndexBucket bucket, Set<Integer> allowed, Set<Integer> ignored) {
        boolean allowing = !allowed.isEmpty();
        boolean ignoring = !ignored.isEmpty();

        Map<Integer, TargetStructure> targetStructures = response.getTargetStructures();
        QueryStructure queryStructure = response.getQuery().getQueryStructure();
        Map<Integer, TargetStructure> processed = new HashMap<>();

        if (targetStructures == null) {
            // first generation: all the paths are valid
            while (bucket.hasNextStructure()) {
                bucket.moveStructure();
                int structureIndex = bucket.getStructureIndex();
                // filtering is enough at this point as it will affect all later operations accordingly
                if ((ignoring && ignored.contains(structureIndex)) || (allowing && !allowed.contains(structureIndex))) {
                    continue;
                }
                int[] occurrencePositions = bucket.getOccurrencePositions();
                TargetStructure targetStructure = new TargetStructure(structureIndex, bucket, occurrencePositions);

                processed.put(structureIndex, targetStructure);
            }
        } else {
            // subsequent generations
            int pathGeneration = response.incrementAndGetPathGeneration();

            // generate overlap profile
            Overlap[] overlapProfile = new Overlap[pathGeneration];
            for (int i = 0; i < pathGeneration; i++) {
                // defined by query structure, known LabelSelections
                overlapProfile[i] = Overlap.ofResiduePairIdentifiers((IndexSelectionResiduePairIdentifier) queryStructure.getResiduePairIdentifiers().get(i), (IndexSelectionResiduePairIdentifier) queryStructure.getResiduePairIdentifiers().get(pathGeneration));
            }

            while (bucket.hasNextStructure()) {
                bucket.moveStructure();
                int structureIndex = bucket.getStructureIndex();
                // no paths for this bucket
                if (targetStructures.containsKey(structureIndex)) {
                    continue;
                }
                TargetStructure targetStructure = targetStructures.get(structureIndex);

                while (bucket.hasNextOccurrence()) {
                    targetStructure.consume(...);
                    bucket.moveOccurrence();
                }

                // TODO reject target structures if not expanded
                // TODO prune target structures
                processed.put(structureIndex, targetStructure);
            }
        }

        response.setTargetStructures(processed);
    }
}
