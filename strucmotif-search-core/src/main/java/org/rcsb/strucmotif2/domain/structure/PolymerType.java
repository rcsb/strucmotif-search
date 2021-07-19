package org.rcsb.strucmotif2.domain.structure;

/**
 * Reports the polymer type for a component. Used to determine the correct constructor during model creation.
 */
public enum PolymerType {
    /**
     * Amino acid chain.
     */
    AMINO_ACID,
    /**
     * Nucleotide chain.
     */
    NUCLEOTIDE
}
