package org.rcsb.strucmotif2.domain.structure;

import org.rcsb.strucmotif2.domain.Transformation;
import org.rcsb.strucmotif2.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif2.math.Algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A residue in a chain. Basically either an amino acid or nucleotide (represented by a subclass). Just like a
 * {@link Chain}, the corresponding {@link Atom} instances are constructed lazily until they are requested for the first
 * time.
 */
public class Residue {
    private final ResidueIdentifier residueIdentifier;
    private final List<Atom> atoms;

    /**
     * Construct a new residue.
     * @param residueIdentifier its identifier
     * @param atoms all atoms in this residue
     * @param transformation the transformation (may be identity/neutral)
     */
    Residue(ResidueIdentifier residueIdentifier, List<Atom> atoms, double[][] transformation) {
        this.residueIdentifier = residueIdentifier;

        boolean neutral = Arrays.deepEquals(Transformation.IDENTITY_MATRIX_4D, transformation);
        if (neutral) {
            this.atoms = atoms;
        } else {
            this.atoms = new ArrayList<>(atoms.size());
            for (Atom atom : atoms) {
                this.atoms.add(new Atom(atom.getAtomIdentifier(), Algebra.multiply4d(transformation, atom.getCoord())));
            }
        }
    }

    /**
     * Address coordinates of an atom in a safe fashion.
     * @param atom the atom to handle
     * @return its coords or null if atom was null
     */
    double[] coord(Atom atom) {
        return atom != null ? atom.getCoord() : null;
    }

    /**
     * Report the identifier of this component.
     * @return the {@link ResidueIdentifier}
     */
    public ResidueIdentifier getResidueIdentifier() {
        return residueIdentifier;
    }

    /**
     * Access to the coordinates of the backbone.
     * @return a double[] on success, otherwise null
     */
    public double[] getBackboneCoordinates() {
        /*
        This implementation is 'generic'. Will succeed even when polymorphism cannot be used because the residue type
        doesnt point to the correct subclass. Achieved by just trying to return CA or C4' by crossing our fingers. Will
        return null if this fails. Subclasses will override for better performance and more precise control.
         */
        Atom ca = findAtomUnsafe("CA");
        if (ca != null) {
            return ca.getCoord();
        }
        Atom c4 = findAtomUnsafe("C4'");
        if (c4 != null) {
            return c4.getCoord();
        }

        return null;
    }

    /**
     * Access to the coordinates of the side-chain.
     * @return a double[] on success, otherwise null
     */
    public double[] getSideChainCoordinates() {
        /*
        This implementation is 'generic'. Will succeed even when polymorphism cannot be used because the residue type
        doesnt point to the correct subclass. Achieved by just trying to return CB or C1' by crossing our fingers. Will
        return null if this fails. Subclasses will override for better performance and more precise control.
         */
        Atom cb = findAtomUnsafe("CB");
        if (cb != null) {
            return cb.getCoord();
        }
        Atom c1 = findAtomUnsafe("C1'");
        if (c1 != null) {
            return c1.getCoord();
        }

        return null;
    }

    /**
     * All atoms associated to this residue.
     * @return a list of atoms
     */
    public List<Atom> getAtoms() {
        return atoms;
    }

    /**
     * Access an atom by name.
     * @param atomName what to look for
     * @return this first atom matching this name, null if absent
     */
    Atom findAtomUnsafe(String atomName) {
        // stream-based approach is called heavily and seems to degenerate performance during lookup generation
        for (Atom atom : atoms) {
            if (atom.getAtomIdentifier().getLabelAtomId().equals(atomName)) {
                return atom;
            }
        }
        return null;
    }

    /**
     * Convenience method to access an atom by its name.
     * @param atomName the 'label_atom_id' of interset
     * @return an optional that maybe wraps the first matched atom (or remained empty)
     */
    public Optional<Atom> findAtom(String atomName) {
        for (Atom atom : atoms) {
            if (atom.getAtomIdentifier().getLabelAtomId().equals(atomName)) {
                return Optional.of(atom);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Residue residue = (Residue) o;
        return Objects.equals(residueIdentifier, residue.residueIdentifier) &&
                Objects.equals(atoms, residue.atoms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(residueIdentifier, atoms);
    }

    @Override
    public String toString() {
        return residueIdentifier.toString();
    }
}
