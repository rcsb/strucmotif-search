package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;

import java.util.List;

/**
 * Pruning motifs is advantageous as it saves time (fewer lookups) and avoids too rigid criteria enforced during search.
 */
public interface MotifPruner {
    /**
     * Perform pruning operation (e.g. extract all {@link ResiduePairOccurrence} instances from a structure and prune
     * the resulting graph).
     * @param residueGraph the graph to operate on
     * @return the list of occurrences which describe this structure
     */
    List<ResiduePairOccurrence> prune(ResidueGraph residueGraph);
}
