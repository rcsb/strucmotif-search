package org.rcsb.strucmotif.domain.selection;

import java.util.Objects;

/**
 * Identifies a residue unambiguously by struct_oper_id, and component index (in and only in the reduced/optimized
 * archive representation). A highly specific way to reference residues used internally by the inverted index.
 */
public class IndexSelection implements ResidueSelection {
    private final String structOperId;
    private final int index;

    public IndexSelection(int index) {
        this("1", index);
    }

    public IndexSelection(String structOperId, int index) {
        this.structOperId = structOperId;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getStructOperId() {
        return structOperId;
    }

    @Override
    public boolean test(String labelAsymId, int labelSeqId, int index) {
        return this.index == index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSelection that = (IndexSelection) o;
        return index == that.index &&
                Objects.equals(structOperId, that.structOperId);
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
