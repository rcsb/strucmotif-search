package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;

/**
 * Pruning motifs is advantageous as it saves time (less lookups) and avoids too rigid criteria enforced during search.
 */
public interface MotifPruner {
    /**
     * Perform pruning operation (e.g. extract all {@link ResiduePairOccurrence} instances from a structure and prune
     * the resulting graph).
     * @param structure the structure from which {@link ResiduePairOccurrence} instances will be extracted
     * @return the set of occurrences which describe this structure
     */
    List<ResiduePairOccurrence> prune(Structure structure);
}
