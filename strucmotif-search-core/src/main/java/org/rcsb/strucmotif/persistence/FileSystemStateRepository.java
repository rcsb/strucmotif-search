package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Revision;
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
    private static final String DELIMITER = ",";
    private final Path knownPath;
    private final Path dirtyPath;

    public FileSystemStateRepository(MotifSearchConfig motifSearchConfig) {
        Path rootPath = Paths.get(motifSearchConfig.getRootPath());
        this.knownPath = rootPath.resolve(MotifSearchConfig.STATE_KNOWN_LIST);
        this.dirtyPath = rootPath.resolve(MotifSearchConfig.STATE_DIRTY_LIST);
    }

    @Override
    public Collection<Pair<StructureIdentifier, Revision>> selectKnown() {
        try {
            return Files.lines(knownPath)
                    .map(line -> line.split(DELIMITER))
                    .map(split -> new Pair<>(new StructureIdentifier(split[0]), new Revision(Integer.parseInt(split[1]), Integer.parseInt(split[2]))))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<StructureIdentifier> selectDirty() {
        if (Files.exists(dirtyPath)) {
            return select(dirtyPath);
        } else {
            return Collections.emptySet();
        }
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
    public void insertKnown(Collection<Pair<StructureIdentifier, Revision>> additions) {
        try {
            FileWriter writer = new FileWriter(knownPath.toFile(), true);
            for (Pair<StructureIdentifier, Revision> addition : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = addition.getFirst().getPdbId() + DELIMITER + addition.getSecond().getMajor() + DELIMITER + addition.getSecond().getMinor() + "\n";
                writer.append(update);
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
    public void deleteDirty(Collection<StructureIdentifier> removals) {
        if (Files.exists(dirtyPath)) {
            delete(removals, dirtyPath);
        } else {
            try {
                Files.createFile(dirtyPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void delete(Collection<StructureIdentifier> removals, Path destination) {
        Set<String> identifiers = removals.stream()
                .map(StructureIdentifier::getPdbId)
                .collect(Collectors.toSet());
        try {
            String output = Files.lines(destination)
                    .filter(line -> identifiers.stream().noneMatch(line::startsWith))
                    .collect(Collectors.joining("\n"));
            Files.write(destination, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
