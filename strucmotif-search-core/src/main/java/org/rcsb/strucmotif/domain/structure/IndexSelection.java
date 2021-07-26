package org.rcsb.strucmotif.domain.structure;

import java.util.Objects;

/**
 * Selects a residue by its index in the source structure file.
 */
public class IndexSelection implements Selection {
    private final String structOperId;
    private final int index;

    /**
     * Constructs an IndexSelection.
     * @param structOperId the operator
     * @param index the index
     */
    public IndexSelection(String structOperId, int index) {
        this.structOperId = structOperId;
        this.index = index;
    }

    @Override
    public String getStructOperId() {
        return structOperId;
    }

    /**
     * The index of the referenced residue.
     * @return an int
     */
    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSelection that = (IndexSelection) o;
        return index == that.index &&
                structOperId.equals(that.structOperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(structOperId, index);
    }

    @Override
    public String toString() {
        return structOperId + "-" + index;
    }
}
