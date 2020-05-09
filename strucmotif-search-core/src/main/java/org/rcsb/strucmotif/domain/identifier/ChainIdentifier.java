package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * An unique identifier for chains. Keeps track if this chain was created by applying a transformation (during
 * bioassembly handling).
 */
public class ChainIdentifier {
    private final String labelAsymId;
    private final String authAsymId;
    private final int assemblyId;

    public ChainIdentifier(String labelAsymId, String authAsymId, int assemblyId) {
        this.labelAsymId = labelAsymId;
        this.authAsymId = authAsymId;
        this.assemblyId = assemblyId;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    public String getAuthAsymId() {
        return authAsymId;
    }

    public int getAssemblyId() {
        return assemblyId;
    }

    public boolean isOriginal() {
        return assemblyId == 1;
    }

    @Override
    public String toString() {
        // auth_asym_id are always String, assemblyIds are always int, concat is safe
        return authAsymId + assemblyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainIdentifier that = (ChainIdentifier) o;
        return assemblyId == that.assemblyId &&
                Objects.equals(labelAsymId, that.labelAsymId) &&
                Objects.equals(authAsymId, that.authAsymId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelAsymId, authAsymId, assemblyId);
    }
}
