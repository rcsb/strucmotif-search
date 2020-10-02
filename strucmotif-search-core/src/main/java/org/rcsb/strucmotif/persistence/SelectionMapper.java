package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;

/**
 * Map between {@link ResidueSelection} instances.
 * @param <S> the source
 * @param <D> the destination
 */
public interface SelectionMapper<S extends ResidueSelection, D extends ResidueSelection> {
    /**
     * Convert the source selection into the destination representation.
     * @param context the structure processed
     * @param source the original selection
     * @return the corresponding selection of the desired destination type
     */
    List<D> select(StructureIdentifier context, List<S> source);

    /**
     * Register a new structure to be mappable.
     * @param structure what to add
     */
    void insert(Structure structure);

    /**
     * Drop mapping information on a particular structure.
     * @param structureIdentifier the identifier
     */
    void delete(StructureIdentifier structureIdentifier);
}
