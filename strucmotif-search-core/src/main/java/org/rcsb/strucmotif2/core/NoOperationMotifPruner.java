package org.rcsb.strucmotif2.core;

import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.rcsb.strucmotif2.domain.ResidueGraph;
import org.rcsb.strucmotif2.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif2.domain.structure.Structure;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Don't prune query motif. Merely extract all {@link ResiduePairOccurrence} instances.
 */
@Service
public class NoOperationMotifPruner implements MotifPruner {
    private final MotifSearchConfig motifSearchConfig;

    /**
     * Injectable constructor.
     * @param motifSearchConfig app config
     */
    public NoOperationMotifPruner(MotifSearchConfig motifSearchConfig) {
        this.motifSearchConfig = motifSearchConfig;
    }

    @Override
    public List<ResiduePairOccurrence> prune(Structure structure) {
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff(), true);

        return residueGraph.residuePairOccurrencesSequential()
                .collect(Collectors.toList());
    }
}
