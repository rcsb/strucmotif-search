package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;

/**
 * A {@link ResiduePairIdentifier} based on {@link IndexSelection} instances, created by the residue graph.
 */
public class IndexResiduePairIdentifier implements ResiduePairIdentifier {
    private final IndexSelection indexSelection1;
    private final IndexSelection indexSelection2;

    /**
     * Construct an identifier from 2 IndexSelections.
     * @param indexSelection1 the first datum
     * @param indexSelection2 the second datum
     */
    public IndexResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2) {
        this.indexSelection1 = indexSelection1;
        this.indexSelection2 = indexSelection2;
    }

    @Override
    public int getIndex1() {
        return indexSelection1.getIndex();
    }

    @Override
    public String getStructOperId1() {
        return indexSelection1.getStructOperId();
    }

    @Override
    public int getIndex2() {
        return indexSelection2.getIndex();
    }

    @Override
    public String getStructOperId2() {
        return indexSelection2.getStructOperId();
    }

    @Override
    public IndexSelection getIndexSelection1() {
        return indexSelection1;
    }

    @Override
    public IndexSelection getIndexSelection2() {
        return indexSelection2;
    }
}
