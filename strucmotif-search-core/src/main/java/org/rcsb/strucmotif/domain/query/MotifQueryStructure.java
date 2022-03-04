package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.Structure;

/**
 * A query structure when searching for motifs.
 */
public class MotifQueryStructure implements QueryStructure {
    private final String structureIdentifier;
    private final Structure structure;

    /**
     * Construct a motif query structure.
     * @param structureIdentifier the identifier
     * @param structure the structure
     */
    public MotifQueryStructure(String structureIdentifier, Structure structure) {
        this.structureIdentifier = structureIdentifier;
        this.structure = structure;
    }

    /**
     * Get the identifier of this query structure.
     *
     * @return a String
     */
    @Override
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Delegate to wrapped structure.
     *
     * @return the structure instance
     */
    @Override
    public Structure getStructure() {
        return structure;
    }
}
