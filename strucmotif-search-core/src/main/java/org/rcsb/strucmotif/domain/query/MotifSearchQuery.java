package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;

/**
 * The immutable container for a structural motif query.
 */
public class MotifSearchQuery implements SearchQuery<MotifParameters, MotifQueryStructure> {
    private final MotifQueryStructure queryStructure;
    private final List<EnrichedMotifDefinition> motifDefinitions;
    private final MotifParameters parameters;

    /**
     * Construct a motif search query.
     * @param structureIdentifier the referenced structure
     * @param structure structure data
     * @param motifDefinitions the motifs to consider
     * @param parameters query parameters
     */
    public MotifSearchQuery(String structureIdentifier,
                            Structure structure,
                            List<EnrichedMotifDefinition> motifDefinitions,
                            MotifParameters parameters) {
        this.queryStructure = new MotifQueryStructure(structureIdentifier, structure);
        this.motifDefinitions = motifDefinitions;
        this.parameters = parameters;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    @Override
    public MotifQueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All associated motif definitions.
     * @return a collection of motifs
     */
    public List<EnrichedMotifDefinition> getMotifDefinitions() {
        return motifDefinitions;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    @Override
    public MotifParameters getParameters() {
        return parameters;
    }
}
