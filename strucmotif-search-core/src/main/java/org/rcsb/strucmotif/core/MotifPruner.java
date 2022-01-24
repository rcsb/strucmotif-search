package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;
import java.util.Map;

/**
 * Pruning motifs is advantageous as it saves time (fewer lookups) and avoids too rigid criteria enforced during search.
 */
public interface MotifPruner {
    /**
     * Perform pruning operation (e.g. extract all {@link ResiduePairOccurrence} instances from a structure and prune
     * the resulting graph).
     * @param structure the structure from which {@link ResiduePairOccurrence} instances will be extracted
     * @param labelSelections the residue identifiers
     * @param residues the manifested residues to work with, a subset of the structure
     * @return the set of occurrences which describe this structure
     */
    List<ResiduePairOccurrence> prune(Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues);
}
