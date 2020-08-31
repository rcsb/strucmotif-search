package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.nio.file.Path;
import java.util.List;

@Singleton
public class FileSystemUpdateStateManagerImpl implements UpdateStateManager {
    private static final Path ARCHIVE_LIST = MotifSearch.ARCHIVE_LIST;
    private static final Path RESIDUE_LIST = MotifSearch.RESIDUE_LIST;
    private static final Path INDEX_LIST = MotifSearch.INDEX_LIST;

    @Override
    public List<StructureIdentifier> getArchiveEntries() {
        return null;
    }

    @Override
    public List<StructureIdentifier> getResidueDBEntries() {
        return null;
    }

    @Override
    public List<StructureIdentifier> getInvertedIndexEntries() {
        return null;
    }

    @Override
    public void insertArchiveEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void insertResidueDBEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void insertInvertedIndexEntries(List<StructureIdentifier> additions) {

    }

    @Override
    public void removeArchiveEntries(List<StructureIdentifier> removals) {

    }

    @Override
    public void removeResidueDBEntries(List<StructureIdentifier> removals) {

    }

    @Override
    public void removeInvertedIndexEntries(List<StructureIdentifier> removals) {

    }
}
