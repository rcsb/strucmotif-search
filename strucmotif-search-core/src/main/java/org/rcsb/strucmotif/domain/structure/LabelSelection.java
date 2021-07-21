package org.rcsb.strucmotif.domain.structure;

import java.util.Objects;

/**
 * Identifies a residue unambiguously by label_asym_id, struct_oper_id, and label_seq_id. Basically the 'natural' way to
 * reference residues.
 */
public class LabelSelection implements Selection {
    private final String labelAsymId;
    private final String structOperId;
    private final int labelSeqId;

    public LabelSelection(String labelAsymId, int labelSeqId) {
        this(labelAsymId, "1", labelSeqId);
    }

    /**
     * Construct a label selection.
     * @param labelAsymId the chain of interest
     * @param structOperId the operation of interest
     * @param labelSeqId the sequence position of interest
     */
    public LabelSelection(String labelAsymId, String structOperId, int labelSeqId) {
        this.labelAsymId = labelAsymId;
        this.structOperId = structOperId;
        this.labelSeqId = labelSeqId;
    }

    /**
     * The chain of interest. Corresponds to 'label_asym_id' in mmCIF.
     * @return a String
     */
    public String getLabelAsymId() {
        return labelAsymId;
    }

    @Override
    public String getStructOperId() {
        return structOperId;
    }

    /**
     * The sequence position of interest. Corresponds to 'label_seq_id' in mmCIF.
     * @return an int
     */
    public int getLabelSeqId() {
        return labelSeqId;
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
