package org.rcsb.strucmotif.domain.selection;

import java.util.Objects;

/**
 * Identifies a residue unambiguously by assembly_id, and component index (in and only in the reduced/optimized
 * archive representation). A highly specific way to reference residues used internally by the inverted index. Of no
 * general relevance. Just painful to map to the original naming schemes like auth or label...
 */
public class IndexSelection implements Selection {
    private final int assemblyId;
    private final int index;

    public IndexSelection(int index) {
        this(1, index);
    }

    public IndexSelection(int assemblyId, int index) {
        this.assemblyId = assemblyId;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int getAssemblyId() {
        return assemblyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexSelection that = (IndexSelection) o;
        return index == that.index &&
                assemblyId == that.assemblyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, assemblyId);
    }

    @Override
    public String toString() {
        return  assemblyId + "-" + index;
    }
}
