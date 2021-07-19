package org.rcsb.strucmotif2.domain;

import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;

import java.util.List;
import java.util.Map;

/**
 * Reports global properties of a structure.
 */
public class StructureInformation {
    private final StructureIdentifier structureIdentifier;
    private final Revision revision;
    private final Map<String, List<String>> assemblyInformation;

    /**
     * Wraps info on a structure.
     * @param structureIdentifier entry id
     * @param revision version
     * @param assemblyInformation present assemblies
     */
    public StructureInformation(StructureIdentifier structureIdentifier, Revision revision, Map<String, List<String>> assemblyInformation) {
        this.structureIdentifier = structureIdentifier;
        this.revision = revision;
        this.assemblyInformation = assemblyInformation;
    }

    /**
     * The {@link StructureIdentifier} of this entry.
     * @return a structure identifier
     */
    public StructureIdentifier getStructureIdentifier() {
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
