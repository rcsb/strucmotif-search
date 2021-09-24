package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    public List<ResiduePairOccurrence> prune(Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues) {
        ResidueGraph residueGraph = new ResidueGraph(structure, labelSelections, residues, motifSearchConfig.getSquaredDistanceCutoff(), true, motifSearchConfig.isUndefinedAssemblies());

        return residueGraph.residuePairOccurrencesSequential()
                .collect(Collectors.toList());
    }
}
