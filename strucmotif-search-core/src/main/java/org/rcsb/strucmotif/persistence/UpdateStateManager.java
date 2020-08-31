package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

public interface UpdateStateManager {
    List<StructureIdentifier> getArchiveEntries();

    List<StructureIdentifier> getResidueDBEntries();

    List<StructureIdentifier> getInvertedIndexEntries();

    void insertArchiveEntries(List<StructureIdentifier> additions);

    void insertResidueDBEntries(List<StructureIdentifier> additions);

    void insertInvertedIndexEntries(List<StructureIdentifier> additions);

    void removeArchiveEntries(List<StructureIdentifier> removals);

    void removeResidueDBEntries(List<StructureIdentifier> removals);

    void removeInvertedIndexEntries(List<StructureIdentifier> removals);
}
