package org.rcsb.strucmotif.domain.structure;

/**
 * A selection within a Structure.
 */
public interface Selection {
    /**
     * The operator applied to the original coordinates.
     * @return an expression based on struct_oper_id
     */
    String getStructOperId();
}
