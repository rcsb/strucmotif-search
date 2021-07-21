package org.rcsb.strucmotif.domain.structure;

import java.util.Objects;

public class IndexSelection implements Selection {
    private final String structOperId;
    private final int index;

    public IndexSelection(String structOperId, int index) {
        this.structOperId = structOperId;
        this.index = index;
    }

    @Override
    public String getStructOperId() {
        return structOperId;
    }

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
