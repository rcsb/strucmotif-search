package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * An unique identifier for structures.
 */
public class StructureIdentifier {
    private final String pdbId;

    public StructureIdentifier(String pdbId) {
        this.pdbId = pdbId;
    }

    public String getPdbId() {
        return pdbId;
    }

    @Override
    public String toString() {
        return pdbId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructureIdentifier that = (StructureIdentifier) o;
        return pdbId.equalsIgnoreCase(that.pdbId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pdbId.toLowerCase());
    }
}
