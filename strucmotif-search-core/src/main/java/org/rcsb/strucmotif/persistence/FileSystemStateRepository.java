package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Revision;
import org.rcsb.strucmotif.domain.StructureInformation;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class FileSystemStateRepository implements StateRepository {
    private static final String TOP_LEVEL_DELIMITER = ",";
    private static final String ASSEMBLY_INFORMATION_DELIMITER = ";";
    private final Path knownPath;
    private final Path dirtyPath;
    // maps from struct_oper_id to all assemblies this transformation is part of
    private final Map<StructureIdentifier, Map<String, Set<String>>> reverseAssemblyInformation;

    public FileSystemStateRepository(MotifSearchConfig motifSearchConfig) {
        Path rootPath = Paths.get(motifSearchConfig.getRootPath());
        this.knownPath = rootPath.resolve(MotifSearchConfig.STATE_KNOWN_LIST);
        this.dirtyPath = rootPath.resolve(MotifSearchConfig.STATE_DIRTY_LIST);
        this.reverseAssemblyInformation = loadAssemblyInformation();
    }

    private Map<StructureIdentifier, Map<String, Set<String>>> loadAssemblyInformation() {
        Collection<StructureInformation> data = selectKnown();
        return data.stream()
                .map(s -> {
                    StructureIdentifier structureIdentifier = s.getStructureIdentifier();
                    Map<String, List<String>> assemblyInformation = s.getAssemblyInformation();
                    Map<String, Set<String>> reversed = new HashMap<>();
                    for (Map.Entry<String, List<String>> partial : assemblyInformation.entrySet()) {
                        String assemblyId = partial.getKey();
                        for (String structOperId : partial.getValue()) {
                            Set<String> mappedAssemblyIds = reversed.computeIfAbsent(structOperId, e -> new HashSet<>());
                            mappedAssemblyIds.add(assemblyId);
                        }
                    }
                    return new Pair<>(structureIdentifier, reversed);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public Map<String, Set<String>> selectAssemblyMap(StructureIdentifier structureIdentifier) {
        return reverseAssemblyInformation.get(structureIdentifier);
    }

    @Override
    public Collection<StructureInformation> selectKnown() {
        try {
            return Files.lines(knownPath)
                    .map(line -> line.split(TOP_LEVEL_DELIMITER))
                    .map(this::handleKnownSplit)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    private StructureInformation handleKnownSplit(String[] split) {
        StructureIdentifier structureIdentifier = new StructureIdentifier(split[0]);
        Revision revision = new Revision(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        Map<String, List<String>> assemblyInformation = IntStream.range(2, split.length)
                .mapToObj(i -> {
                    String[] assemblySplit = split[i].split(ASSEMBLY_INFORMATION_DELIMITER);
                    String assemblyId = assemblySplit[0];
                    List<String> operList = Arrays.stream(assemblySplit, 1, assemblySplit.length)
                            .collect(Collectors.toList());
                    return new Pair<>(assemblyId, operList);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        return new StructureInformation(structureIdentifier, revision, assemblyInformation);
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
    public void insertKnown(Collection<StructureInformation> additions) {
        try {
            FileWriter writer = new FileWriter(knownPath.toFile(), true);
            for (StructureInformation addition : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = addition.getStructureIdentifier().getPdbId() + TOP_LEVEL_DELIMITER +
                        addition.getRevision().getMajor() + TOP_LEVEL_DELIMITER +
                        addition.getRevision().getMinor() + TOP_LEVEL_DELIMITER +
                        addition.getAssemblyInformation().entrySet().stream()
                                .map(entry -> entry.getKey() + ASSEMBLY_INFORMATION_DELIMITER +
                                        String.join(ASSEMBLY_INFORMATION_DELIMITER, entry.getValue()))
                                .collect(Collectors.joining(TOP_LEVEL_DELIMITER)) + "\n";
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
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.write(destination, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
