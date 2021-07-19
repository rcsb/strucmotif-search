package org.rcsb.strucmotif2.domain.identifier;

import java.util.Objects;

/**
 * A unique identifier for atoms.
 */
public class AtomIdentifier {
    private final String labelAtomId;
    private final int id;

    /**
     * Identify an individual atom by its name and serial.
     * @param labelAtomId the name
     * @param id the serial
     */
    public AtomIdentifier(String labelAtomId, int id) {
        this.labelAtomId = labelAtomId;
        this.id = id;
    }

    /**
     * Name of this atom.
     * @return a String
     */
    public String getLabelAtomId() {
        return labelAtomId;
    }

    /**
     * Serial of this atom.
     * @return an int
     */
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return labelAtomId + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomIdentifier that = (AtomIdentifier) o;
        return id == that.id &&
                Objects.equals(labelAtomId, that.labelAtomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, labelAtomId);
    }

    /**
     * Check if two AtomIdentifiers reference the same atom (i.e. one with the same name). Useful to find compatible
     * atoms during alignment.
     * @param other another instance
     * @return true if the same atom name
     */
    public boolean describeSameAtom(AtomIdentifier other) {
        return labelAtomId.equals(other.getLabelAtomId());
    }
}
