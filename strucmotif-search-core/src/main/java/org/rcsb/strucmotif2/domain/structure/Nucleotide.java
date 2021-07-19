package org.rcsb.strucmotif2.domain.structure;

import org.rcsb.strucmotif2.domain.identifier.ResidueIdentifier;

import java.util.List;

/**
 * A nucleotide in a structure.
 */
public class Nucleotide extends Residue {
    private final Atom c1prime;
    private final Atom c4prime;

    /**
     * Construct a new nucleotide.
     * @param residueIdentifier its identifier
     * @param atoms associated atoms
     * @param transformation transformation to apply
     */
    Nucleotide(ResidueIdentifier residueIdentifier, List<Atom> atoms, double[][] transformation) {
        super(residueIdentifier, atoms, transformation);
        this.c1prime = findAtomUnsafe("C1'");
        this.c4prime = findAtomUnsafe("C4'");
    }

    /**
     * The polymer anchor for nucleotides.
     * @return an atom
     */
    public Atom getC4prime() {
        return c4prime;
    }

    /**
     * The interaction anchor for nucleotides.
     * @return an atom
     */
    public Atom getC1prime() {
        return c1prime;
    }

    @Override
    public double[] getBackboneCoordinates() {
        return coord(getC4prime());
    }

    @Override
    public double[] getSideChainCoordinates() {
        return coord(getC1prime());
    }
}
