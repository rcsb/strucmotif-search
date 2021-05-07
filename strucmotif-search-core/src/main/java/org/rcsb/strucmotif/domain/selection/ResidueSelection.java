package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.structure.Residue;

/**
 * Identifies a {@link Residue} uniquely in a complete structure.
 */
public interface ResidueSelection {
    /**
     * The struct_oper_id expression to select.
     * @return a String
     */
    String getStructOperId();

    /**
     * Test whether this some candidate matches this selection.
     * TODO this makes implementations 'aware' of one another, still better than not using polymorphism (maybe?)
     * @param labelAsymId presented chain - might be ignored
     * @param labelSeqId presented seq id - might be ignored
     * @param index presented index - might be ignored
     * @return <code>true</code> if the selection matches the presented information
     */
    boolean test(String labelAsymId, int labelSeqId, int index);
}
