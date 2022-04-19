package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.StructureSearchContext;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.IndexSelectionResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.InvertedIndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.query.StructureParameters;
import org.rcsb.strucmotif.domain.query.StructureQueryStructure;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default strucmotif-search target assembler.
 */
@Service
public class TargetAssemblerImpl implements TargetAssembler {
    private static final Logger logger = LoggerFactory.getLogger(TargetAssemblerImpl.class);
    private final ThreadPool threadPool;
    private final StructureIndexProvider structureIndexProvider;

    /**
     * Injectable constructor.
     * @param threadPool thread pool
     * @param structureIndexProvider maps from structureIdentifiers to indices
     */
    @Autowired
    public TargetAssemblerImpl(ThreadPool threadPool, StructureIndexProvider structureIndexProvider) {
        this.threadPool = threadPool;
        this.structureIndexProvider = structureIndexProvider;
    }

    @Override
    public void assemble(StructureSearchContext context) throws ExecutionException, InterruptedException {
        StructureQuery query = context.getQuery();
        StructureQueryStructure queryStructure = query.getQueryStructure();
        StructureParameters parameters = query.getParameters();
        InvertedIndex invertedIndex = context.getInvertedIndex();
        StructureSearchResult result = context.getResult();
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

        Set<Integer> searchSpace = structureIndexProvider.selectByResultsContentType(query.getResultsContentType());
        Set<Integer> allowed = query.getAllowedStructures()
                .stream()
                .map(structureIndexProvider::selectStructureIndex)
                .collect(Collectors.toSet());
        Set<Integer> ignored = query.getExcludedStructures()
                .stream()
                .map(structureIndexProvider::selectStructureIndex)
                .collect(Collectors.toSet());

        result.getTimings().pathsStart();
        // retrieve target identifiers per query motif descriptor
        int steps = queryStructure.getResiduePairOccurrences().size();
        for (int i = 0; i < steps; i++) {
            long s = System.nanoTime();
            ResiduePairOccurrence residuePairOccurrence = queryStructure.getResiduePairOccurrences().get(i);
            ResiduePairDescriptor residuePairDescriptor = residuePairOccurrence.getResiduePairDescriptor();

            // sort into target structures
            Map<Integer, InvertedIndexResiduePairIdentifier[]> residuePairIdentifiers = threadPool.submit(() -> residuePairOccurrence.residuePairDescriptorsByTolerance(backboneDistanceTolerance, sideChainDistanceTolerance, angleTolerance, exchanges)
                    .flatMap(descriptor -> select(invertedIndex, descriptor, searchSpace, allowed, ignored))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, TargetAssemblerImpl::concat))).get();

            // TODO try to avoid object creation
            // TODO try to consume stream directly
            consume(context, residuePairIdentifiers);

            // update allowed set for next iteration
            if (i + 1 < steps) {
                Set<Integer> keys = result.getTargetStructures().keySet();
                if (i == 0 && allowed.isEmpty()) {
                    allowed.addAll(keys);
                } else {
                    allowed.removeIf(v -> !keys.contains(v));
                }
            }

            logger.info("[{}] Consumed {} in {} ms - {} valid target structures remaining",
                    context.getId(),
                    residuePairDescriptor,
                    (System.nanoTime() - s) / 1000 / 1000,
                    result.getTargetStructures().size());

            if (i > 0 && i + 1 < steps && allowed.isEmpty()) {
                logger.info("[{}] No more valid extensions - terminating early", context.getId());
                break;
            }
        }
        result.getTimings().pathsStop();

        int pathCount = result.getTargetStructures().values().stream().mapToInt(TargetStructure::getNumberOfValidPaths).sum();
        int structureCount = result.getTargetStructures().size();
        logger.info("[{}] Found {} valid paths ({} target structures) in {} ms",
                context.getId(),
                pathCount,
                structureCount,
                result.getTimings().getPathsTime());
        result.setNumberOfPaths(pathCount);
        result.setNumberOfTargetStructures(structureCount);
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private Stream<Pair<Integer, InvertedIndexResiduePairIdentifier[]>> select(InvertedIndex invertedIndex, ResiduePairDescriptor descriptor, Set<Integer> searchSpace, Set<Integer> allowed, Set<Integer> ignored) {
        InvertedIndexBucket bucket = invertedIndex.select(descriptor);
        @SuppressWarnings("unchecked")
        Pair<Integer, InvertedIndexResiduePairIdentifier[]>[] out = new Pair[bucket.getStructureCount()];

        int i = 0;
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            int structureIndex = bucket.getStructureIndex();

            // if there is a whitelist, this entry has to occur therein
            if (!allowed.isEmpty() && !allowed.contains(structureIndex)) {
                continue;
            }
            // cannot occur in blacklist
            if (ignored.contains(structureIndex)) {
                continue;
            }
            // check 'global' search space - might be null if it's desired to skip this step, might be empty if legitimately no structures match
            if (searchSpace != null && !searchSpace.contains(structureIndex)) {
                continue;
            }

            int[] occurrencePositions = bucket.getOccurrencePositions();
            InvertedIndexResiduePairIdentifier[] identifiers = new InvertedIndexResiduePairIdentifier[occurrencePositions.length];

            for (int j = 0; j < occurrencePositions.length; j++) {
                identifiers[j] = createResiduePairIdentifier(bucket, descriptor.isFlipped(), occurrencePositions[j]);
            }

            out[i] = new Pair<>(structureIndex, identifiers);
            i++;
        }

        return Arrays.stream(out).limit(i);
    }

    private InvertedIndexResiduePairIdentifier createResiduePairIdentifier(InvertedIndexBucket bucket, boolean flipped, int i) {
        if (!flipped) {
            return new InvertedIndexResiduePairIdentifier(bucket.getIndex(i), bucket.getIndex(i + 1), bucket.getStructOperId(i), bucket.getStructOperId(i + 1));
        } else {
            return new InvertedIndexResiduePairIdentifier(bucket.getIndex(i + 1), bucket.getIndex(i), bucket.getStructOperId(i + 1), bucket.getStructOperId(i));
        }
    }

    private void consume(StructureSearchContext context, Map<Integer, InvertedIndexResiduePairIdentifier[]> data) throws ExecutionException, InterruptedException {
        StructureQuery query = context.getQuery();
        StructureSearchResult result = context.getResult();
        Map<Integer, TargetStructure> targetStructures = result.getTargetStructures();
        StructureQueryStructure queryStructure = query.getQueryStructure();

        if (targetStructures == null) {
            // first generation: all the paths are valid
            result.setTargetStructures(data.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> new TargetStructure(v.getKey(), v.getValue()))));
        } else {
            // subsequent generations
            int pathGeneration = result.incrementAndGetPathGeneration();

            // generate overlap profile
            Overlap[] overlapProfile = new Overlap[pathGeneration];
            for (int i = 0; i < pathGeneration; i++) {
                // defined by query structure, known LabelSelections
                overlapProfile[i] = Overlap.ofResiduePairIdentifiers((IndexSelectionResiduePairIdentifier) queryStructure.getResiduePairIdentifiers().get(i), (IndexSelectionResiduePairIdentifier) queryStructure.getResiduePairIdentifiers().get(pathGeneration));
            }

            // focus on valid target structures as this set should be smaller
            result.setTargetStructures(threadPool.submit(() -> targetStructures.entrySet()
                            .parallelStream()
                            .filter(entry -> {
                                InvertedIndexResiduePairIdentifier[] residuePairIdentifiers = data.get(entry.getKey());
                                // candidate must have valid path to extend from previous generation
                                if (residuePairIdentifiers == null) {
                                    return false;
                                }

                                // append target structure by whatever the new target identifiers for this structure have to offer
                                return entry.getValue().consume(residuePairIdentifiers, overlapProfile);
                            })
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .get());
        }
    }
}
