package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.InternalMotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The immutable container for a structural motif query.
 */
public class MotifSearchQuery {
    private final InternalMotifSearch internalMotifSearch;
    private final QueryStructure queryStructure;
    private final Parameters parameters;
    private final Map<LabelSelection, Set<ResidueType>> exchanges;
    private final Collection<StructureIdentifier> whitelist;
    private final Collection<StructureIdentifier> blacklist;

    MotifSearchQuery(InternalMotifSearch internalMotifSearch,
                     Structure structure,
                     Parameters parameters,
                     Map<LabelSelection, Set<ResidueType>> exchanges,
                     Collection<StructureIdentifier> whitelist,
                     Collection<StructureIdentifier> blacklist) {
        this.internalMotifSearch = internalMotifSearch;
        List<ResiduePairOccurrence> residuePairOccurrences = parameters.getMotifPruner().prune(structure);
        this.queryStructure = new QueryStructure(structure, residuePairOccurrences);
        this.parameters = parameters;
        this.exchanges = exchanges;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    public QueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * All specified exchanges.
     * @return a map (key: label selector, value: all allowed component types)
     */
    public Map<LabelSelection, Set<ResidueType>> getExchanges() {
        return exchanges;
    }

    public Collection<StructureIdentifier> getWhitelist() {
        return whitelist;
    }

    public Collection<StructureIdentifier> getBlacklist() {
        return blacklist;
    }

    public MotifSearchResult run() {
        return internalMotifSearch.performSearch(this);
    }
}
