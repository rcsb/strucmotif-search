package org.rcsb.strucmotif.domain.structure;

import java.util.List;
import java.util.Map;

/**
 * Reports global properties of a structure.
 */
public class StructureInformation {
    private final String structureIdentifier;
    private final Revision revision;
    private final Map<String, List<String>> assemblyInformation;

    /**
     * Wraps info on a structure.
     * @param structureIdentifier entry id
     * @param revision version
     * @param assemblyInformation present assemblies
     */
    public StructureInformation(String structureIdentifier, Revision revision, Map<String, List<String>> assemblyInformation) {
        this.structureIdentifier = structureIdentifier;
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
    public Map<String, List<String>> getAssemblyInformation() {
        return assemblyInformation;
    }
}
