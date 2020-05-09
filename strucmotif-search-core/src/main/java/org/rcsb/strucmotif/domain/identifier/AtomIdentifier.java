package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * An unique identifier for atoms.
 */
public class AtomIdentifier {
    private final String labelAtomId;
    private final int id;
    private final String labelAltId;

    public AtomIdentifier(String labelAtomId, int id) {
        this(labelAtomId, id, null);
    }

    public AtomIdentifier(String labelAtomId, int id, String labelAltId) {
        this.labelAtomId = labelAtomId;
        this.id = id;
        this.labelAltId = labelAltId;
    }

    public String getLabelAtomId() {
        return labelAtomId;
    }

    public int getId() {
        return id;
    }

    public boolean isAlternativeLocation() {
        return labelAltId != null && !"A".equals(labelAltId);
    }

    public String getLabelAltId() {
        return labelAltId;
    }

    @Override
    public String toString() {
        return isAlternativeLocation() ? labelAtomId + id + labelAltId : labelAtomId + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomIdentifier that = (AtomIdentifier) o;
        return id == that.id &&
                Objects.equals(labelAtomId, that.labelAtomId) &&
                Objects.equals(labelAltId, that.labelAltId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, labelAtomId, labelAltId);
    }

    public boolean describeSameAtom(AtomIdentifier other) {
        if (!labelAtomId.equals(other.getLabelAtomId())) {
            return false;
        }
        return Objects.equals(labelAltId, other.labelAltId);
    }
}
