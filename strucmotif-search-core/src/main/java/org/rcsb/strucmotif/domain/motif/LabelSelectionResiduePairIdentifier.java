package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

public class LabelSelectionResiduePairIdentifier implements ResiduePairIdentifier {
    private final LabelSelection labelSelection1;
    private final LabelSelection labelSelection2;

    public LabelSelectionResiduePairIdentifier(LabelSelection labelSelection1, LabelSelection labelSelection2) {
        this.labelSelection1 = labelSelection1;
        this.labelSelection2 = labelSelection2;
    }

    @Override
    public String getLabelAsymId1() {
        return labelSelection1.getLabelAsymId();
    }

    @Override
    public String getStructOperId1() {
        return labelSelection1.getStructOperId();
    }

    @Override
    public int getLabelSeqId1() {
        return labelSelection1.getLabelSeqId();
    }

    @Override
    public String getLabelAsymId2() {
        return labelSelection2.getLabelAsymId();
    }

    @Override
    public String getStructOperId2() {
        return labelSelection2.getStructOperId();
    }

    @Override
    public int getLabelSeqId2() {
        return labelSelection2.getLabelSeqId();
    }

    @Override
    public LabelSelection getLabelSelection1() {
        return labelSelection1;
    }

    @Override
    public LabelSelection getLabelSelection2() {
        return labelSelection2;
    }
}
