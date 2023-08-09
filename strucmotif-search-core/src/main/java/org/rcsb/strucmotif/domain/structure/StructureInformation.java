package org.rcsb.strucmotif.domain.structure;

/**
 * Reports global properties of a structure.
 * @param structureIdentifier entry id
 * @param structureIndex globally unique identifier of this entry
 * @param majorRevision version info
 * @param minorRevision version info
 */
public record StructureInformation(String structureIdentifier, int structureIndex, int majorRevision, int minorRevision) {}
