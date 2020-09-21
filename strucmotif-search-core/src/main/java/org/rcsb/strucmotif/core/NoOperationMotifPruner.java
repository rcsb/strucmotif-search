package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.ResidueGraph;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Don't prune query motif. Merely extract all {@link ResiduePairOccurrence} instances.
 */
@Service
public class NoOperationMotifPruner implements MotifPruner {
    private final MotifSearchConfig motifSearchConfig;

    public NoOperationMotifPruner(MotifSearchConfig motifSearchConfig) {
        this.motifSearchConfig = motifSearchConfig;
    }

    @Override
    public List<ResiduePairOccurrence> prune(Structure structure) {
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        return residueGraph.residuePairOccurrencesSequential()
                .collect(Collectors.toList());
    }
}
