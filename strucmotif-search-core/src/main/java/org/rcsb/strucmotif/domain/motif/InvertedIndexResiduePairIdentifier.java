package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;

/**
 * A {@link ResiduePairIdentifier} defined by data from the inverted index. Just a wrapper for the raw data array.
 */
public class InvertedIndexResiduePairIdentifier implements ResiduePairIdentifier {
    private final int index1;
    private final int index2;
    private final String structOperId1;
    private final String structOperId2;

    public InvertedIndexResiduePairIdentifier(int index1, int index2, String structOperId1, String structOperId2) {
        this.index1 = index1;
        this.index2 = index2;
        this.structOperId1 = structOperId1;
        this.structOperId2 = structOperId2;
    }

    @Override
    public int getIndex1() {
        return index1;
    }

    @Override
    public int getIndex2() {
        return index2;
    }

    @Override
    public String getStructOperId1() {
        return structOperId1;
    }

    @Override
    public String getStructOperId2() {
        return structOperId2;
    }

    @Override
    public IndexSelection getIndexSelection1() {
        return new IndexSelection(getStructOperId1(), getIndex1());
    }

    @Override
    public IndexSelection getIndexSelection2() {
        return new IndexSelection(getStructOperId2(), getIndex2());
    }
}
