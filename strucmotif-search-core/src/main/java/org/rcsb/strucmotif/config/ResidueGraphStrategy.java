package org.rcsb.strucmotif.config;

/**
 * Controls what residue pairs get detected.
 */
public enum ResidueGraphStrategy {
    /**
     * Only report contacts of deposited coordinates.
     */
    DEPOSITED,
    /**
     * Reports contacts between deposited coordinates as well as all residues that are in contact with the deposited
     * chain(s). This is a superset of `DEPOSITED`.
     */
    RESIDUES_IN_CONTACT,
    /**
     * Report contacts between deposited coordinates as well as all chains that are in contact with the deposited
     * chain(s). This is a superset of `RESIDUES_IN_CONTACT`.
     */
    CHAINS_IN_CONTACT,
    /**
     * Index absolutely everything.
     */
    ALL
}
