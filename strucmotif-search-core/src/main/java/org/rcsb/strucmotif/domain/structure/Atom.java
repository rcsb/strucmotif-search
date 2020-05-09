package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;

import java.util.Arrays;
import java.util.Objects;

/**
 * An atom. Coordinates + identifier.
 */
public class Atom {
    private final AtomIdentifier atomIdentifier;
    private final double[] coord;

    /**
     * Construct a new atom.
     * @param atomIdentifier its identifier
     * @param coord its position
     */
    Atom(AtomIdentifier atomIdentifier, double[] coord) {
        this.atomIdentifier = atomIdentifier;
        this.coord = coord;
    }

    /**
     * Reports the identifier of this atom.
     * @return a {@link AtomIdentifier}
     */
    public AtomIdentifier getAtomIdentifier() {
        return atomIdentifier;
    }

    /**
     * Reports the position of this atom.
     * @return a double[]
     */
    public double[] getCoord() {
        return coord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atom atom = (Atom) o;
        return Objects.equals(atomIdentifier, atom.atomIdentifier) &&
                Arrays.equals(coord, atom.coord);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(atomIdentifier);
        result = 31 * result + Arrays.hashCode(coord);
        return result;
    }

    @Override
    public String toString() {
        return atomIdentifier.toString();
    }
}
