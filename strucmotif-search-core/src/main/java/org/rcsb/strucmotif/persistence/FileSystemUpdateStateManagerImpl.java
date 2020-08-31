package org.rcsb.strucmotif.persistence;

import com.google.inject.Singleton;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class FileSystemUpdateStateManagerImpl implements UpdateStateManager {
    @Override
    public Set<StructureIdentifier> selectArchiveEntries() {
        return select(MotifSearch.ARCHIVE_LIST);
    }

    @Override
    public Set<StructureIdentifier> selectResidueDBEntries() {
        return select(MotifSearch.RESIDUE_LIST);
    }

    @Override
    public Set<StructureIdentifier> selectInvertedIndexEntries() {
        return select(MotifSearch.INDEX_LIST);
    }

    private Set<StructureIdentifier> select(Path path) {
        try {
            return Files.lines(path)
                    .map(StructureIdentifier::new)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            // happens for full load
            return Collections.emptySet();
        }
    }

    @Override
    public void insertArchiveEntries(Set<StructureIdentifier> additions) {
        insert(additions, MotifSearch.ARCHIVE_LIST);
    }

    @Override
    public void insertResidueDBEntries(Set<StructureIdentifier> additions) {
        insert(additions, MotifSearch.RESIDUE_LIST);
    }

    @Override
    public void insertInvertedIndexEntries(Set<StructureIdentifier> additions) {
        insert(additions, MotifSearch.INDEX_LIST);
    }

    private void insert(Set<StructureIdentifier> additions, Path path) {
        try {
            FileWriter processedWriter = new FileWriter(path.toFile(), true);
            for (StructureIdentifier structureIdentifier : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = structureIdentifier.getPdbId() + "\n";
                processedWriter.append(update);
            }
            processedWriter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteArchiveEntries(Set<StructureIdentifier> removals) {
        delete(removals, MotifSearch.ARCHIVE_LIST);
    }

    @Override
    public void deleteResidueDBEntries(Set<StructureIdentifier> removals) {
        delete(removals, MotifSearch.RESIDUE_LIST);
    }

    @Override
    public void deleteInvertedIndexEntries(Set<StructureIdentifier> removals) {
        delete(removals, MotifSearch.INDEX_LIST);
    }

    private void delete(Set<StructureIdentifier> removals, Path path) {
        Set<String> identifiers = removals.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toSet());
        try {
            String output = Files.lines(path)
                    .filter(line -> !identifiers.contains(line))
                    .collect(Collectors.joining("\n"));
            Files.write(path, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
