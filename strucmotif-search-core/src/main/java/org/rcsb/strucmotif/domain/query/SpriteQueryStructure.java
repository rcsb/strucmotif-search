package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.Structure;

/**
 * A query structure wraps a {@link Structure} and provides additional functionality needed to employ it as motif
 * definition during a structural motif query job.
 */
public class SpriteQueryStructure implements QueryStructure {
    private final String structureIdentifier;
    private final Structure structure;

    SpriteQueryStructure(String structureIdentifier, Structure structure) {
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
