package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.query.SearchSpace;

import java.util.Set;

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
     * Maps between structure identifier and index.
     * @param structureIdentifier the String
     * @return the int
     */
    int selectStructureIndex(String structureIdentifier);

    /**
     * Get the structure index associated to this identifier. If none exists a new one will be created.
     * @param structureIdentifier the structure identifier
     * @return a unique int that maps to this identifier
     */
    int selectOrMintStructureIndex(String structureIdentifier);

    /**
     * Get the next available structureIndex. This may reuse a previously assigned index that is no longer referenced.
     * @return an int that is guaranteed to uniquely identify a structure in the inverted index
     */
    int nextStructureIndex();

    /**
     * Check if this structure identifier is known.
     * @param structureIdentifier the String
     * @return true if this key has a mapping
     */
    boolean containsKey(String structureIdentifier);

    /**
     * Check if this structure index is known.
     * @param structureIndex the int
     * @return true if this key has a mapping
     */
    boolean containsKey(int structureIndex);

    /**
     * Get all registered indices of a particular type.
     * @param searchSpace search space identifier (PDB entries, computed structure models, everything)
     * @return a collection of all relevant structure indices
     */
    Set<Integer> selectBySearchSpace(SearchSpace searchSpace);
}
