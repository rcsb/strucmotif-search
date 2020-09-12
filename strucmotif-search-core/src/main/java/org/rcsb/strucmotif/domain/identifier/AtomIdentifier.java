package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * An unique identifier for atoms.
 */
public class AtomIdentifier {
    private final String labelAtomId;
    private final int id;

    public AtomIdentifier(String labelAtomId, int id) {
        this.labelAtomId = labelAtomId;
        this.id = id;
    }

    public String getLabelAtomId() {
        return labelAtomId;
    }

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

    public boolean describeSameAtom(AtomIdentifier other) {
        return labelAtomId.equals(other.getLabelAtomId());
    }
}
