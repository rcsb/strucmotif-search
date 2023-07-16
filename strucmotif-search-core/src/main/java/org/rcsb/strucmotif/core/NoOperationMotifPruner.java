package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Don't prune query motif. Merely extract all {@link ResiduePairOccurrence} instances.
 */
@Service
public class NoOperationMotifPruner implements MotifPruner {
    @Override
    public List<ResiduePairOccurrence> prune(ResidueGraph residueGraph) {
        return residueGraph.residuePairOccurrencesSequential()
                // sort all edges by weight
                .sorted(Comparator.comparingInt(mo -> ResiduePairDescriptor.getBackboneDistance(mo.getResiduePairDescriptor()).ordinal()))
                .collect(Collectors.toList());
    }
}
