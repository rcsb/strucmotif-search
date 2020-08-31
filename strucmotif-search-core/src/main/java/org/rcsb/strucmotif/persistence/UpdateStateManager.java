package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Set;

public interface UpdateStateManager {
    Set<StructureIdentifier> selectArchiveEntries();

    Set<StructureIdentifier> selectResidueDBEntries();

    Set<StructureIdentifier> selectInvertedIndexEntries();

    void insertArchiveEntries(Set<StructureIdentifier> additions);

    void insertResidueDBEntries(Set<StructureIdentifier> additions);

    void insertInvertedIndexEntries(Set<StructureIdentifier> additions);

    void deleteArchiveEntries(Set<StructureIdentifier> removals);

    void deleteResidueDBEntries(Set<StructureIdentifier> removals);

    void deleteInvertedIndexEntries(Set<StructureIdentifier> removals);
}
