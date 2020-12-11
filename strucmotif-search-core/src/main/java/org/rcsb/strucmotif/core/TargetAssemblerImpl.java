package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class TargetAssemblerImpl implements TargetAssembler {
    private static final Logger logger = LoggerFactory.getLogger(TargetAssemblerImpl.class);
    private final InvertedIndex invertedIndex;
    private final ThreadPool threadPool;

    @Autowired
    public TargetAssemblerImpl(InvertedIndex invertedIndex, ThreadPool threadPool) {
        this.invertedIndex = invertedIndex;
        this.threadPool = threadPool;
    }

    @Override
    public void assemble(MotifSearchResult response) throws ExecutionException, InterruptedException  {
        MotifSearchQuery query = response.getQuery();
        QueryStructure queryStructure = query.getQueryStructure();
        Parameters parameters = query.getParameters();
        Map<LabelSelection, Set<ResidueType>> exchanges = query.getExchanges();
        boolean whitelist = !query.getWhitelist().isEmpty();
        boolean blacklist = !query.getBlacklist().isEmpty();

        response.getTimings().pathsStart();
        // retrieve target identifiers per query motif descriptor
        for (ResiduePairOccurrence residuePairOccurrence : queryStructure.getResiduePairOccurrences()) {
            ResiduePairDescriptor residuePairDescriptor = residuePairOccurrence.getResiduePairDescriptor();

            // sort into target structures
            Map<StructureIdentifier, ResiduePairIdentifier[]> residuePairIdentifiers;
            // asked to honor entry-level white- or blacklist
            if (whitelist || blacklist) {
                residuePairIdentifiers = residuePairOccurrence.residuePairDescriptorsByTolerance(parameters, exchanges)
                        .flatMap(invertedIndex::select)
                        // if there is a whitelist, this entry has to occur therein
                        .filter(pair -> !whitelist || query.getWhitelist().contains(pair.getFirst()))
                        // cannot occur in blacklist
                        .filter(pair -> !query.getBlacklist().contains(pair.getFirst()))
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, TargetAssemblerImpl::concat));
            } else {
                // standard mode: accepted everybody
                residuePairIdentifiers = residuePairOccurrence.residuePairDescriptorsByTolerance(parameters, exchanges)
                        .flatMap(invertedIndex::select)
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, TargetAssemblerImpl::concat));
            }

            // consume by target structures
            consume(response, residuePairIdentifiers);

            logger.debug("[{}] Consumed {} - {} valid target structures remaining",
                    response.getQuery().hashCode(),
                    residuePairDescriptor,
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

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private void consume(MotifSearchResult response, Map<StructureIdentifier, ResiduePairIdentifier[]> data) throws ExecutionException, InterruptedException {
        Map<StructureIdentifier, TargetStructure> targetStructures = response.getTargetStructures();
        QueryStructure queryStructure = response.getQuery().getQueryStructure();

        if (targetStructures == null) {
            // first generation: all the paths are valid
            response.setTargetStructures(data.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> new TargetStructure(v.getKey(), v.getValue()))));
        } else {
            // subsequent generations
            int pathGeneration = response.incrementAndGetPathGeneration();

            // generate overlap profile
            Overlap[] overlapProfile = new Overlap[pathGeneration];
            for (int i = 0; i < pathGeneration; i++) {
                overlapProfile[i] = Overlap.ofResiduePairIdentifiers(queryStructure.getResiduePairIdentifiers().get(i), queryStructure.getResiduePairIdentifiers().get(pathGeneration));
            }

            // focus on valid target structures as this set should be smaller
            response.setTargetStructures(threadPool.submit(() -> targetStructures.entrySet()
                    .parallelStream()
                    .filter(entry -> {
                        ResiduePairIdentifier[] residuePairIdentifiers = data.get(entry.getKey());
                        // candidate must have valid path to extend from previous generation
                        if (residuePairIdentifiers == null) {
                            return false;
                        }

                        // append target structure by whatever the new target identifiers for this structure have to offer
                        return entry.getValue().consume(residuePairIdentifiers, overlapProfile);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .get());

            if (targetStructures.isEmpty()) {
                throw new IllegalArgumentException("did not find any candidates after " + pathGeneration + " generations");
            }
        }
    }
}
