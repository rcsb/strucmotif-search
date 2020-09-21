package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * A unique identifier for chains. Keeps track if this chain was created by applying a transformation (during generation
 * of bioassemblies).
 */
public class ChainIdentifier {
    private final String labelAsymId;
    private final int assemblyId;

    public ChainIdentifier(String labelAsymId, int assemblyId) {
        this.labelAsymId = labelAsymId;
        this.assemblyId = assemblyId;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    public int getAssemblyId() {
        return assemblyId;
    }

    public boolean isOriginal() {
        return assemblyId == 1;
    }

    @Override
    public String toString() {
        // label_asym_id are always String, assemblyIds are always int, concat is safe
        return labelAsymId + assemblyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainIdentifier that = (ChainIdentifier) o;
        return assemblyId == that.assemblyId &&
                Objects.equals(labelAsymId, that.labelAsymId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelAsymId, assemblyId);
    }
}
