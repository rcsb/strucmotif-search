package org.rcsb.strucmotif.domain.selection;

import java.util.Objects;

/**
 * Identifies a component unambiguously by label_asym_id, assembly_id, and label_seq_id. Basically the 'natural' way to
 * reference residues.
 */
public class LabelSelection implements Selection {
    private final String labelAsymId;
    private final int assemblyId;
    private final int labelSeqId;

    public LabelSelection(String labelAsymId, int assemblyId, int labelSeqId) {
        this.labelAsymId = labelAsymId;
        this.assemblyId = assemblyId;
        this.labelSeqId = labelSeqId;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    @Override
    public int getAssemblyId() {
        return assemblyId;
    }

    public int getLabelSeqId() {
        return labelSeqId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelSelection that = (LabelSelection) o;
        return assemblyId == that.assemblyId &&
                labelSeqId == that.labelSeqId &&
                Objects.equals(labelAsymId, that.labelAsymId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelAsymId, assemblyId, labelSeqId);
    }

    @Override
    public String toString() {
        return labelAsymId + assemblyId + "-" + labelSeqId;
    }
}
