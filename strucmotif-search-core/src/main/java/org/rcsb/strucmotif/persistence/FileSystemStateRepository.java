package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileSystemStateRepository implements StateRepository {
    private final Path knownPath;
    private final Path supportedPath;
    private final Path dirtyPath;

    public FileSystemStateRepository(MotifSearchConfig motifSearchConfig) {
        Path rootPath = Paths.get(motifSearchConfig.getRootPath());
        this.knownPath = rootPath.resolve(MotifSearchConfig.STATE_KNOWN_LIST);
        this.supportedPath = rootPath.resolve(MotifSearchConfig.STATE_SUPPORTED_LIST);
        this.dirtyPath = rootPath.resolve(MotifSearchConfig.STATE_DIRTY_LIST);
    }

    @Override
    public Collection<StructureIdentifier> selectKnown() {
        return select(knownPath);
    }

    @Override
    public Collection<StructureIdentifier> selectSupported() {
        return select(supportedPath);
    }

    @Override
    public Collection<StructureIdentifier> selectDirty() {
        return select(dirtyPath);
    }

    private Set<StructureIdentifier> select(Path source) {
        try {
            return Files.lines(source)
                    .map(StructureIdentifier::new)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void insertKnown(Collection<StructureIdentifier> additions) {
        insert(additions, knownPath);
    }

    @Override
    public void insertSupported(Collection<StructureIdentifier> additions) {
        insert(additions, supportedPath);
    }

    @Override
    public void insertDirty(Collection<StructureIdentifier> additions) {
        insert(additions, dirtyPath);
    }

    private void insert(Collection<StructureIdentifier> additions, Path destination) {
        try {
            FileWriter writer = new FileWriter(destination.toFile(), true);
            for (StructureIdentifier structureIdentifier : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = structureIdentifier.getPdbId() + "\n";
                writer.append(update);
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteKnown(Collection<StructureIdentifier> removals) {
        delete(removals, knownPath);
    }

    @Override
    public void deleteSupported(Collection<StructureIdentifier> removals) {
        delete(removals, supportedPath);
    }

    @Override
    public void deleteDirty(Collection<StructureIdentifier> removals) {
        delete(removals, dirtyPath);
    }

    private void delete(Collection<StructureIdentifier> removals, Path destination) {
        Set<String> identifiers = removals.stream()
                .map(StructureIdentifier::getPdbId)
                .collect(Collectors.toSet());
        try {
            String output = Files.lines(destination)
                    .filter(line -> !identifiers.contains(line))
                    .collect(Collectors.joining("\n"));
            Files.write(destination, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
