package org.rcsb.strucmotif.domain.identifier;

import java.util.Objects;

/**
 * Identifies assemblies in the context of a PDB entry.
 */
public class AssemblyIdentifier {
    private final String assemblyId;

    /**
     * Construct an assembly identifier.
     * @param assemblyId the id
     */
    public AssemblyIdentifier(String assemblyId) {
        this.assemblyId = assemblyId;
    }

    /**
     * this assembly identifier
     * @return a String
     */
    public String getAssemblyId() {
        return assemblyId;
    }

    @Override
    public String toString() {
        return assemblyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssemblyIdentifier that = (AssemblyIdentifier) o;
        return Objects.equals(assemblyId, that.assemblyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assemblyId);
    }
}
