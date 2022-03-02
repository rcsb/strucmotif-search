package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifSearchConfig;
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
public class AssamSearchQuery implements SearchQuery<AssamParameters, AssamQueryStructure> {
    private final AssamQueryStructure queryStructure;
    private final AssamParameters parameters;
    private final Map<LabelSelection, Set<ResidueType>> exchanges;
    private final Collection<String> whitelist;
    private final Collection<String> blacklist;
    private final StructureDeterminationMethodology structureDeterminationMethodology;

    public AssamSearchQuery(String structureIdentifier,
                            Structure structure,
                            List<LabelSelection> labelSelections,
                            List<Map<LabelAtomId, float[]>> residues,
                            AssamParameters parameters,
                            Map<LabelSelection, Set<ResidueType>> exchanges,
                            Collection<String> whitelist,
                            Collection<String> blacklist,
                            StructureDeterminationMethodology structureDeterminationMethodology,
                            MotifSearchConfig motifSearchConfig) {
        ResidueGraph residueGraph = new ResidueGraph(structure, labelSelections, residues, motifSearchConfig, true);
        List<ResiduePairOccurrence> residuePairOccurrences = parameters.getMotifPruner().prune(residueGraph);
        this.queryStructure = new AssamQueryStructure(structureIdentifier, structure, labelSelections, residues, residuePairOccurrences, exchanges);
        this.parameters = parameters;
        this.exchanges = exchanges;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
        this.structureDeterminationMethodology = structureDeterminationMethodology;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    @Override
    public AssamQueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    @Override
    public AssamParameters getParameters() {
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
    public Collection<String> getWhitelist() {
        return whitelist;
    }

    /**
     * Returns disallowed entries of the search space.
     * @return a collection of entries that will be ignored
     */
    public Collection<String> getBlacklist() {
        return blacklist;
    }

    /**
     * Access to the applied search space.
     * @return ALL, PDB, or MODELS
     */
    public StructureDeterminationMethodology getStructureDeterminationMethodology() {
        return structureDeterminationMethodology;
    }
}
