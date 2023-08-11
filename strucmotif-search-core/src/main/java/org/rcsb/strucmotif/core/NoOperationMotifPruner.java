package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Don't prune query motif. Merely extract all {@link ResiduePairOccurrence} instances.
 */
@Service
public class NoOperationMotifPruner implements MotifPruner {
    /**
     * Default constructor.
     */
    public NoOperationMotifPruner() {
    }

    @Override
    public List<ResiduePairOccurrence> prune(ResidueGraph residueGraph, Map<Integer, Set<ResidueType>> exchanges) {
        List<ResiduePairOccurrence> original = residueGraph.residuePairOccurrencesSequential().collect(Collectors.toList());
        return ResiduePairOccurrence.sort(original, exchanges);
    }
}
