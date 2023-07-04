package org.rcsb.strucmotif.domain.structure;

import java.util.Objects;

/**
 * Identifies a residue unambiguously by label_asym_id, struct_oper_id, and label_seq_id. Basically, the 'natural' way
 * to reference residues.
 */
public record LabelSelection(String labelAsymId, String structOperId, int labelSeqId) {
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
