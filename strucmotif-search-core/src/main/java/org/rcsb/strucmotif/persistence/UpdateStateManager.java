package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

public interface UpdateStateManager {
    Collection<StructureIdentifier> selectArchiveEntries();

    Collection<StructureIdentifier> selectResidueDBEntries();

    Collection<StructureIdentifier> selectInvertedIndexEntries();

    void insertArchiveEntries(Collection<StructureIdentifier> additions);

    void insertResidueDBEntries(Collection<StructureIdentifier> additions);

    void insertInvertedIndexEntries(Collection<StructureIdentifier> additions);

    void deleteArchiveEntries(Collection<StructureIdentifier> removals);

    void deleteResidueDBEntries(Collection<StructureIdentifier> removals);

    void deleteInvertedIndexEntries(Collection<StructureIdentifier> removals);
}
