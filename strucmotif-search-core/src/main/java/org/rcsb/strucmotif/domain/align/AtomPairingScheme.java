package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;

import java.util.function.Predicate;

/**
 * Schemes on which atoms (by name) to use to align residues. Each entry is actually a {@link Predicate} which tests
 * atom names and returns true if fall into that particular scheme.
 */
public enum AtomPairingScheme implements Predicate<LabelAtomId> {
    /**
     * Use everything.
     */
    ALL(s -> true),
    /**
     * Use alpha carbons only.
     */
    ALPHA_CARBON(s -> s.equals(LabelAtomId.CA) ||
            s.equals(LabelAtomId.C4_PRIME)),
    /**
     * Use beta carbons only.
     */
    BETA_CARBON(s -> s.equals(LabelAtomId.CB) ||
            s.equals(LabelAtomId.C1_PRIME)),
    /**
     * Use backbone atoms only.
     */
    BACKBONE(s -> s.equals(LabelAtomId.N) || s.equals(LabelAtomId.CA) || s.equals(LabelAtomId.C) ||
            s.equals(LabelAtomId.O) || s.equals(LabelAtomId.P) || s.equals(LabelAtomId.OP1) ||
            s.equals(LabelAtomId.OP2) || s.equals(LabelAtomId.C2_PRIME) || s.equals(LabelAtomId.C3_PRIME) ||
            s.equals(LabelAtomId.O3_PRIME) || s.equals(LabelAtomId.C4_PRIME) || s.equals(LabelAtomId.O4_PRIME) ||
            s.equals(LabelAtomId.C5_PRIME) || s.equals(LabelAtomId.O5_PRIME)),
    /**
     * Use side-chain atoms only.
     */
    SIDE_CHAIN(BACKBONE.predicate.negate()),
    /**
     * Use pseudo-atoms defined by {@link org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor}.
     */
    PSEUDO_ATOMS(s -> s.equals(LabelAtomId.CA) || s.equals(LabelAtomId.CB) ||
            s.equals(LabelAtomId.C4_PRIME) || s.equals(LabelAtomId.C1_PRIME));

    private final Predicate<LabelAtomId> predicate;

    AtomPairingScheme(Predicate<LabelAtomId> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(LabelAtomId s) {
        return predicate.test(s);
    }
}
