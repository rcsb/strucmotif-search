package org.rcsb.strucmotif.domain.selection;

import java.util.Objects;

/**
 * Identifies a residue unambiguously by label_asym_id, struct_oper_id, and label_seq_id. Basically the 'natural' way to
 * reference residues.
 */
public class LabelSelection implements ResidueSelection {
    private final String labelAsymId;
    private final String structOperId;
    private final int labelSeqId;

    public LabelSelection(String labelAsymId, String structOperId, int labelSeqId) {
        this.labelAsymId = labelAsymId;
        this.structOperId = structOperId;
        this.labelSeqId = labelSeqId;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    @Override
    public String getStructOperId() {
        return structOperId;
    }

    public int getLabelSeqId() {
        return labelSeqId;
    }

    @Override
    public boolean test(String labelAsymId, int labelSeqId, int index) {
        return this.labelSeqId == labelSeqId && this.labelAsymId.equals(labelAsymId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelSelection that = (LabelSelection) o;
        return labelSeqId == that.labelSeqId &&
                Objects.equals(labelAsymId, that.labelAsymId) &&
                Objects.equals(structOperId, that.structOperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelAsymId, structOperId, labelSeqId);
    }

    @Override
    public String toString() {
        return labelAsymId + "_" + structOperId + "-" + labelSeqId;
    }
}
