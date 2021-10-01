package org.rcsb.strucmotif.io;

/**
 * Maps between structure indices (a compact int representation that is e.g. stored in the inverted index) and readable
 * Strings (that can be PDB-IDs or longer, namespaced identifiers used by computed structure models).
 */
public interface StructureIndexProvider {
    /**
     * Maps between structure index and identifier.
     * @param structureIndex the int
     * @return the String
     */
    String selectStructureIdentifier(int structureIndex);

    /**
     * Get the next available structureIndex. This may reuse a previously assigned index that is no longer referenced.
     * @return an int that is guaranteed to uniquely identify a structure in the inverted index
     */
    int nextStructureIndex();
}
