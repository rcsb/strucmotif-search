package org.rcsb.strucmotif.domain.structure;

/**
 * Reports global properties of a structure.
 */
public record StructureInformation(String structureIdentifier, int structureIndex, int majorRevision, int minorRevision) {}
