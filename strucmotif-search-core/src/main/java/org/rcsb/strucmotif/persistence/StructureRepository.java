package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.Collection;

public interface StructureRepository {
    Structure select(StructureIdentifier structureIdentifier, Collection<IndexSelection> indexSelections);

    void insert(Structure structure);

    void delete(StructureIdentifier structureIdentifier);
}
