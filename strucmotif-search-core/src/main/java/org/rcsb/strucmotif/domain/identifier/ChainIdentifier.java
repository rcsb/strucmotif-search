package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * A unique identifier for chains. Keeps track if this chain was created by applying a transformation (during generation
 * of bioassemblies).
 */
public class ChainIdentifier {
    private final String labelAsymId;
    private final String structOperId;

    public ChainIdentifier(String labelAsymId, String structOperId) {
        this.labelAsymId = labelAsymId;
        this.structOperId = structOperId;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    public String getStructOperId() {
        return structOperId;
    }

    @Override
    public String toString() {
        // label_asym_id are always String, assemblyIds are always int, concat is safe
        return labelAsymId + "_" + structOperId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainIdentifier that = (ChainIdentifier) o;
        return Objects.equals(labelAsymId, that.labelAsymId) &&
                Objects.equals(structOperId, that.structOperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelAsymId, structOperId);
    }
}
