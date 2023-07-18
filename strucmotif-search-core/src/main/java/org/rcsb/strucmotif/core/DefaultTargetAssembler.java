package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.StructureSearchContext;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.query.StructureParameters;
import org.rcsb.strucmotif.domain.query.StructureQueryStructure;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
import org.rcsb.strucmotif.domain.result.TargetStructure;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default strucmotif-search target assembler.
 */
@Service
public class DefaultTargetAssembler implements TargetAssembler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTargetAssembler.class);
    private final ThreadPool threadPool;
    private final StructureIndexProvider structureIndexProvider;

    /**
     * Injectable constructor.
     * @param threadPool thread pool
     * @param structureIndexProvider maps from structureIdentifiers to indices
     */
    @Autowired
    public DefaultTargetAssembler(ThreadPool threadPool, StructureIndexProvider structureIndexProvider) {
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
        Map<Integer, Set<ResidueType>> exchanges = labelSelectionExchanges.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> {
                    LabelSelection labelSelection = entry.getKey();
                    return queryStructure.getStructure().getResidueIndex(labelSelection);
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
            int residuePairDescriptor = residuePairOccurrence.getResiduePairDescriptor();

            // sort into target structures
            Map<Integer, int[]> residuePairIdentifiers = threadPool.submit(() -> residuePairOccurrence.residuePairDescriptorsByTolerance(backboneDistanceTolerance, sideChainDistanceTolerance, angleTolerance, exchanges)
                    .mapToObj(descriptor -> select(invertedIndex, descriptor, searchSpace, allowed, ignored))
                    .flatMap(Function.identity())
                    .collect(Collectors.toConcurrentMap(Pair::first, Pair::second, DefaultTargetAssembler::concat))).get();

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
                    ResiduePairDescriptor.toString(residuePairDescriptor),
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

    private static int[] concat(int[] first, int[] second) {
        int[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private Stream<Pair<Integer, int[]>> select(InvertedIndex invertedIndex, int descriptor, Set<Integer> searchSpace, Set<Integer> allowed, Set<Integer> ignored) {
        int actualDescriptor = ResiduePairDescriptor.stripFlipBit(descriptor);
        InvertedIndexBucket bucket = invertedIndex.select(actualDescriptor);
        if (bucket == InvertedIndexBucket.EMPTY_BUCKET) {
            return Stream.empty();
        }

        boolean flipped = ResiduePairDescriptor.isFlipped(descriptor);
        // the ugly case which requires the creation of both residuePairs
        boolean ambiguous = ResiduePairDescriptor.isAmbiguous(descriptor);

        @SuppressWarnings("unchecked")
        Pair<Integer, int[]>[] out = new Pair[bucket.getStructureCount()];

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

            int start = bucket.getStartPosition();
            int end = bucket.getEndPosition();
            int n = end - start;
            int[] identifiers;
            if (ambiguous) {
                identifiers = new int[n * 2];

                for (int j = 0; j < n - 1; j = j + 2) {
                    int index1 = bucket.getResidueIndex(start + j);
                    int index2 = bucket.getResidueIndex(start + j + 1);
                    identifiers[2 * j] = index1;
                    identifiers[2 * j + 1] = index2;
                    identifiers[2 * j + 2] = index2;
                    identifiers[2 * j + 3] = index1;
                }
            } else {
                identifiers = new int[n];

                for (int j = 0; j < n - 1; j = j + 2) {
                    int index1 = bucket.getResidueIndex(start + j);
                    int index2 = bucket.getResidueIndex(start + j + 1);
                    if (flipped) {
                        identifiers[j] = index2;
                        identifiers[j + 1] = index1;
                    } else {
                        identifiers[j] = index1;
                        identifiers[j + 1] = index2;
                    }
                }
            }

            out[i++] = new Pair<>(structureIndex, identifiers);
        }

        if (i == 0) {
            return Stream.empty();
        }
        return Arrays.stream(out).limit(i);
    }

    private void consume(StructureSearchContext context, Map<Integer, int[]> data) throws ExecutionException, InterruptedException {
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
                overlapProfile[i] = Overlap.ofResiduePairIdentifiers(queryStructure.getResiduePairIdentifiers().get(i),
                        queryStructure.getResiduePairIdentifiers().get(pathGeneration));
            }

            // focus on valid target structures as this set should be smaller
            result.setTargetStructures(threadPool.submit(() -> targetStructures.entrySet()
                            .parallelStream()
                            .filter(entry -> {
                                int[] residuePairIdentifiers = data.get(entry.getKey());
                                // candidate must have valid path to extend from previous generation
                                if (residuePairIdentifiers == null) {
                                    return false;
                                }

                                // append target structure by whatever the new target identifiers for this structure have to offer
                                return entry.getValue().consume(residuePairIdentifiers, overlapProfile);
                            })
                            .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .get());
        }
    }
}
