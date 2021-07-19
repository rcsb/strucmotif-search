package org.rcsb.strucmotif2.domain;

import java.util.function.Predicate;

/**
 * Schemes on which atoms (by name) to use to align residues. Each entry is actually a {@link Predicate} which tests
 * atom names and returns true if fall into that particular scheme.
 */
public enum AtomPairingScheme implements Predicate<String> {
    /**
     * Use everything.
     */
    ALL(s -> true),
    /**
     * Use alpha carbons only.
     */
    ALPHA_CARBON(s -> s.equals("CA") ||
            s.equals("C4'")),
    /**
     * Use beta carbons only.
     */
    BETA_CARBON(s -> s.equals("CB") ||
            s.equals("C1'")),
    /**
     * Use backbone atoms only.
     */
    BACKBONE(s -> s.equals("N") || s.equals("CA") || s.equals("C") || s.equals("O") ||
            s.equals("P") || s.equals("OP1") || s.equals("OP2") || s.equals("C2'") || s.equals("C3'") || s.equals("O3'") || s.equals("C4'") || s.equals("O4'") || s.equals("C5'") || s.equals("O5'")),
    /**
     * Use side-chain atoms only.
     */
    SIDE_CHAIN(BACKBONE.predicate.negate()),
    /**
     * Use pseudo-atoms defined by {@link org.rcsb.strucmotif2.domain.motif.ResiduePairDescriptor}.
     */
    PSEUDO_ATOMS(s -> s.equals("CA") || s.equals("CB") ||
            s.equals("C4'") || s.equals("C1'"));

    private final Predicate<String> predicate;

    AtomPairingScheme(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(String s) {
        return predicate.test(s);
    }
}
