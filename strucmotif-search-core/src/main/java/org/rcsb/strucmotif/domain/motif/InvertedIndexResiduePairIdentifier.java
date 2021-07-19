package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

public class InvertedIndexResiduePairIdentifier implements ResiduePairIdentifier {
    private final Object[] data;
    private final boolean flipped;

    public InvertedIndexResiduePairIdentifier(Object[] data, boolean flipped) {
        // length 3: { seqId1, seqId2, asymId1 };
        // length 4: { seqId1, seqId2, asymId1, asymId2 };
        // length 5: { seqId1, seqId2, asymId1, structOperId1, structOperId2 };
        // length 6: { seqId1, seqId2, asymId1, asymId2, structOperId1, structOperId2 };
        this.data = data;
        this.flipped = flipped;
    }

    @Override
    public String getLabelAsymId1() {
        if (data.length == 3 || data.length == 5 || !flipped) {
            return (String) data[2];
        }
        return (String) data[3];
    }

    @Override
    public String getStructOperId1() {
        if (data.length < 5) {
            return "1";
        }
        if (data.length == 5 && !flipped) {
            return (String) data[3];
        }
        if (data.length == 5 || (data.length == 6 && !flipped)) {
            return (String) data[4];
        }
        return (String) data[5];
    }

    @Override
    public int getLabelSeqId1() {
        return (int) (!flipped ? data[0] : data[1]);
    }

    @Override
    public String getLabelAsymId2() {
        if (data.length == 3 || data.length == 5 || flipped) {
            return (String) data[2];
        }
        return (String) data[3];
    }

    @Override
    public String getStructOperId2() {
        if (data.length < 5) {
            return "1";
        }
        if (data.length == 5 && flipped) {
            return (String) data[3];
        }
        if (data.length == 5 || (data.length == 6 && flipped)) {
            return (String) data[4];
        }
        return (String) data[5];
    }

    @Override
    public int getLabelSeqId2() {
        return (int) (!flipped ? data[1] : data[0]);
    }

    @Override
    public LabelSelection getLabelSelection1() {
        return new LabelSelection(getLabelAsymId1(), getStructOperId1(), getLabelSeqId1());
    }

    @Override
    public LabelSelection getLabelSelection2() {
        return new LabelSelection(getLabelAsymId2(), getStructOperId2(), getLabelSeqId2());
    }
}
