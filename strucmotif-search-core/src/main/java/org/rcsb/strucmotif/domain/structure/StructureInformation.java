package org.rcsb.strucmotif.domain.structure;

import java.util.Map;
import java.util.Set;

/**
 * Reports global properties of a structure.
 */
public class StructureInformation {
    private final String structureIdentifier;
    private final int structureIndex;
    private final Revision revision;
    private final Map<String, Set<String>> assemblyInformation;

    /**
     * Wraps info on a structure.
     * @param structureIdentifier entry id
     * @param structureIndex globally unique identifier of this entry
     * @param revision version
     * @param assemblyInformation present assemblies
     */
    public StructureInformation(String structureIdentifier, int structureIndex, Revision revision, Map<String, Set<String>> assemblyInformation) {
        this.structureIdentifier = structureIdentifier;
        this.structureIndex = structureIndex;
        this.revision = revision;
        this.assemblyInformation = assemblyInformation;
    }

    /**
     * The ID of this entry.
     * @return a structure identifier
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * An integer that is guaranteed to be a unique identifier for this structure. Used to map between the int
     * representation and a readable String representation (such as a PDB-ID).
     * @return an int
     */
    public int getStructureIndex() {
        return structureIndex;
    }

    /**
     * The {@link Revision} of this entry.
     * @return combination of major and minor version
     */
    public Revision getRevision() {
        return revision;
    }

    /**
     * Reports present assemblies and the struct_oper_ids needed to create them.
     * @return a map between assembly id and a collection of struct_oper_ids (or combinations thereof).
     */
    public Map<String, Set<String>> getAssemblyInformation() {
        return assemblyInformation;
    }
}
