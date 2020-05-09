package org.rcsb.strucmotif.domain.selection;

import java.util.Objects;

/**
 * Identifies a component unambiguously by auth_asym_id, assembly_id, and auth_seq_id. Basically the 'natural' way to
 * reference residues.
 */
public class AuthorSelection implements Selection {
    private final String authAsymId;
    private final int assemblyId;
    private final int authSeqId;
    private final String insCode;

    public AuthorSelection(String authAsymId, int assemblyId, int authSeqId) {
        this(authAsymId, assemblyId, authSeqId, "");
    }

    public AuthorSelection(String authAsymId, int assemblyId, int authSeqId, String insCode) {
        this.authAsymId = authAsymId;
        this.assemblyId = assemblyId;
        this.authSeqId = authSeqId;
        this.insCode = insCode;
    }

    public String getAuthAsymId() {
        return authAsymId;
    }

    @Override
    public int getAssemblyId() {
        return assemblyId;
    }

    public int getAuthSeqId() {
        return authSeqId;
    }

    public String getInsCode() {
        return insCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorSelection that = (AuthorSelection) o;
        return assemblyId == that.assemblyId &&
                authSeqId == that.authSeqId &&
                Objects.equals(authAsymId, that.authAsymId) &&
                Objects.equals(insCode, that.insCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authAsymId, assemblyId, authSeqId, insCode);
    }

    @Override
    public String toString() {
        return authAsymId + assemblyId + "-" + authSeqId + insCode;
    }
}
