package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.selection.AuthorSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.ResidueTypeGrouping;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines a position-specific exchange for a particular position in the query structure.
 */
public class PositionSpecificExchange {
    private final AuthorSelection authorSelection;
    private final Set<ResidueType> residueTypes;

    /**
     * @see PositionSpecificExchange (AuthorSelection, Set)
     * @param authorSelection selector of the referenced position
     * @param residueTypeGrouping grouping of allowed types (must include original type if still allowed)
     */
    public PositionSpecificExchange(AuthorSelection authorSelection, ResidueTypeGrouping residueTypeGrouping) {
        this(authorSelection, residueTypeGrouping.getResidueTypes());
    }

    /**
     * @see PositionSpecificExchange (AuthorSelection, Set)
     * @param authorSelection selector of the referenced position
     * @param residueTypes all allowed types (must include original type if still allowed)
     */
    public PositionSpecificExchange(AuthorSelection authorSelection, ResidueType... residueTypes) {
        this(authorSelection, Stream.of(residueTypes).collect(Collectors.toSet()));
    }

    /**
     * Constructs a new position-specific exchange.
     * @param authorSelection selector of the referenced position
     * @param residueTypes all allowed types (must include original type if still allowed)
     */
    public PositionSpecificExchange(AuthorSelection authorSelection, Set<ResidueType> residueTypes) {
        this.authorSelection = authorSelection;
        this.residueTypes = residueTypes;
    }

    /**
     * The position this exchange references.
     * @return the {@link AuthorSelection} of a residue
     */
    public AuthorSelection getAuthorSelection() {
        return authorSelection;
    }

    /**
     * The set of allowed component types at that position. Must explicitly include the original component type if that
     * should still be allowed.
     * @return all types that can occur at this position
     */
    public Set<ResidueType> getResidueTypes() {
        return residueTypes;
    }
}
