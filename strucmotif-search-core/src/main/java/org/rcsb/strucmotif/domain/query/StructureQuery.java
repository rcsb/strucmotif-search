package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The immutable container for a structural motif query.
 */
public class StructureQuery implements SearchQuery<StructureParameters, StructureQueryStructure> {
    private final StructureQueryStructure queryStructure;
    private final StructureParameters parameters;
    private final Map<LabelSelection, Set<ResidueType>> exchanges;
    private final Collection<String> allowedStructures;
    private final Collection<String> excludedStructures;
    private final Collection<ResultsContentType> resultsContentType;

    /**
     * Construct a structure query.
     * @param structureIdentifier structure identifier
     * @param structure structure data
     * @param labelSelections referenced residues
     * @param residues referenced residue data
     * @param parameters query parameters
     * @param exchanges optional exchanges
     * @param allowedStructures which structure to include
     * @param excludedStructures which structures to exclude
     * @param resultsContentType which provenance to allow
     * @param strucmotifConfig the global config
     */
    public StructureQuery(String structureIdentifier,
                          Structure structure,
                          List<LabelSelection> labelSelections,
                          List<Map<LabelAtomId, float[]>> residues,
                          StructureParameters parameters,
                          Map<LabelSelection, Set<ResidueType>> exchanges,
                          Collection<String> allowedStructures,
                          Collection<String> excludedStructures,
                          Collection<ResultsContentType> resultsContentType,
                          StrucmotifConfig strucmotifConfig) {
        ResidueGraph residueGraph = new ResidueGraph(structure, labelSelections, residues, strucmotifConfig);

        // validate graph and reject motifs with dangling residues or multiple fragments
        if (!residueGraph.isConnected()) {
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }

        List<ResiduePairOccurrence> residuePairOccurrences = parameters.getMotifPruner().prune(residueGraph);
        this.queryStructure = new StructureQueryStructure(structureIdentifier, structure, labelSelections, residues, residuePairOccurrences, exchanges);
        this.parameters = parameters;
        this.exchanges = exchanges;
        this.allowedStructures = allowedStructures;
        this.excludedStructures = excludedStructures;
        this.resultsContentType = resultsContentType;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    @Override
    public StructureQueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    @Override
    public StructureParameters getParameters() {
        return parameters;
    }

    /**
     * All specified exchanges.
     * @return a map (key: label selector, value: all allowed component types)
     */
    public Map<LabelSelection, Set<ResidueType>> getExchanges() {
        return exchanges;
    }

    /**
     * Returns the specified search space - no restrictions apply if empty.
     * @return a collection of the search space
     */
    public Collection<String> getAllowedStructures() {
        return allowedStructures;
    }

    /**
     * Returns disallowed entries of the search space.
     * @return a collection of entries that will be ignored
     */
    public Collection<String> getExcludedStructures() {
        return excludedStructures;
    }

    /**
     * Access to the applied search space.
     * @return a collection containing any combination of EXPERIMENTAL, COMPUTATIONAL
     */
    public Collection<ResultsContentType> getResultsContentType() {
        return resultsContentType;
    }
}
