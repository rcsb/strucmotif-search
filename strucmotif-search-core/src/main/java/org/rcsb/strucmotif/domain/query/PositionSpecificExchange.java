package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines a position-specific exchange for a particular position in the query structure.
 */
public class PositionSpecificExchange {
    private final LabelSelection labelSelection;
    private final Set<ResidueType> residueTypes;

    /**
     * Constructs a new position-specific exchange.
     * @see PositionSpecificExchange (LabelSelection, Set)
     * @param labelSelection selector of the referenced position
     * @param residueTypes all allowed types (must include original type if still allowed)
     */
    public PositionSpecificExchange(LabelSelection labelSelection, ResidueType... residueTypes) {
        this(labelSelection, Stream.of(residueTypes).collect(Collectors.toSet()));
    }

    /**
     * Constructs a new position-specific exchange.
     * @param labelSelection selector of the referenced position
     * @param residueTypes all allowed types (must include original type if still allowed)
     */
    public PositionSpecificExchange(LabelSelection labelSelection, Set<ResidueType> residueTypes) {
        this.labelSelection = labelSelection;
        this.residueTypes = residueTypes;
    }

    /**
     * The position this exchange references.
     * @return the {@link LabelSelection} of a residue
     */
    public LabelSelection getLabelSelection() {
        return labelSelection;
    }

    /**
     * The set of allowed component types at that position. Must explicitly include the original residue type if that
     * should still be allowed.
     * @return all types that can occur at this position
     */
    public Set<ResidueType> getResidueTypes() {
        return residueTypes;
    }
}
