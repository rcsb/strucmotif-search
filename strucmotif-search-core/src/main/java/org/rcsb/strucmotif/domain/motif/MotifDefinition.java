package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Identifier-based representation of motifs.
 */
public class MotifDefinition {
    private final String structureIdentifier;
    private final List<LabelSelection> labelSelections;
    private final Set<PositionSpecificExchange> positionSpecificExchanges;

    public MotifDefinition(String structureIdentifier, List<LabelSelection> labelSelections) {
        this(structureIdentifier, labelSelections, Collections.emptySet());
    }

    public MotifDefinition(String structureIdentifier, List<LabelSelection> labelSelections, Set<PositionSpecificExchange> positionSpecificExchanges) {
        this.structureIdentifier = structureIdentifier;
        this.labelSelections = labelSelections;
        this.positionSpecificExchanges = positionSpecificExchanges;
    }

    /**
     * The structure identifier of this motif.
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * The residues referenced by this motif.
     * @return a collection of LabelSelections
     */
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    /**
     * Potential position-specific exchanges for this motif.
     * @return a collection of exchanges
     */
    public Set<PositionSpecificExchange> getPositionSpecificExchanges() {
        return positionSpecificExchanges;
    }
}
