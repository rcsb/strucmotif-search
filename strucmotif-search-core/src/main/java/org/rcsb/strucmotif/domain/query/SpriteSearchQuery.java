package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;

/**
 * The immutable container for a structural motif query.
 */
public class SpriteSearchQuery implements SearchQuery<SpriteParameters, SpriteQueryStructure> {
    private final SpriteQueryStructure queryStructure;
    private final List<EnrichedMotifDefinition> motifDefinitions;
    private final SpriteParameters parameters;

    public SpriteSearchQuery(String structureIdentifier,
                     Structure structure,
                     List<EnrichedMotifDefinition> motifDefinitions,
                     SpriteParameters parameters) {
        this.queryStructure = new SpriteQueryStructure(structureIdentifier, structure);
        this.motifDefinitions = motifDefinitions;
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

    public List<EnrichedMotifDefinition> getMotifDefinitions() {
        return motifDefinitions;
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
