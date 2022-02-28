package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.Structure;

/**
 * The immutable container for a structural motif query.
 */
public class SpriteSearchQuery implements SearchQuery<SpriteParameters, SpriteQueryStructure> {
    private final SpriteQueryStructure queryStructure;
    private final SpriteParameters parameters;

    SpriteSearchQuery(String structureIdentifier,
                     Structure structure,
                     SpriteParameters parameters) {
        this.queryStructure = new SpriteQueryStructure(structureIdentifier, structure);
        this.parameters = parameters;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    @Override
    public SpriteQueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    @Override
    public SpriteParameters getParameters() {
        return parameters;
    }
}
