package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.Revision;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
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

/**
 * A file-system-based state repository.
 */
@Service
public class StateRepositoryImpl implements StateRepository {
    private static final String TOP_LEVEL_DELIMITER = ",";
    private static final String ASSEMBLY_INFORMATION_DELIMITER = ";";
    private final Path knownPath;
    private final Path dirtyPath;
    // maps from struct_oper_id to all assemblies this transformation is part of
    private final Map<String, Map<String, Set<String>>> reverseAssemblyInformation;

    /**
     * Construct a state repository instance.
     * @param motifSearchConfig the config
     */
    public StateRepositoryImpl(MotifSearchConfig motifSearchConfig) {
        Path rootPath = Paths.get(motifSearchConfig.getRootPath());
        this.knownPath = rootPath.resolve(MotifSearchConfig.STATE_KNOWN_LIST);
        this.dirtyPath = rootPath.resolve(MotifSearchConfig.STATE_DIRTY_LIST);
        this.reverseAssemblyInformation = loadAssemblyInformation();
    }

    private Map<String, Map<String, Set<String>>> loadAssemblyInformation() {
        Collection<StructureInformation> data = selectKnown();
        return data.stream()
                .map(s -> {
                    String structureIdentifier = s.getStructureIdentifier();
                    Map<String, Set<String>> assemblyInformation = s.getAssemblyInformation();
                    Map<String, Set<String>> reversed = new HashMap<>();
                    for (Map.Entry<String, Set<String>> partial : assemblyInformation.entrySet()) {
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
    public Map<String, Set<String>> selectAssemblyMap(String structureIdentifier) {
        return reverseAssemblyInformation.get(structureIdentifier);
    }

    @Override
    public Collection<StructureInformation> selectKnown() {
        try {
            return Files.lines(knownPath)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split(TOP_LEVEL_DELIMITER))
                    .map(this::handleKnownSplit)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Split a line into a structure information object.
     * @param split the raw line
     * @return a structure information container
     */
    protected StructureInformation handleKnownSplit(String[] split) {
        String structureIdentifier = new String(split[0]);
        Revision revision = new Revision(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        Map<String, Set<String>> assemblyInformation = IntStream.range(3, split.length)
                .mapToObj(i -> {
                    String[] assemblySplit = split[i].split(ASSEMBLY_INFORMATION_DELIMITER);
                    String assemblyId = assemblySplit[0];
                    Set<String> operList = Arrays.stream(assemblySplit, 1, assemblySplit.length)
                            .collect(Collectors.toSet());
                    return new Pair<>(assemblyId, operList);
                })
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        return new StructureInformation(structureIdentifier, revision, assemblyInformation);
    }

    @Override
    public Collection<String> selectDirty() {
        if (Files.exists(dirtyPath)) {
            return select(dirtyPath);
        } else {
            return Collections.emptySet();
        }
    }

    private Set<String> select(Path source) {
        try {
            return Files.lines(source)
                    .filter(line -> !line.isBlank())
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
                String update = addition.getStructureIdentifier() + TOP_LEVEL_DELIMITER +
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
    public void insertDirty(Collection<String> additions) {
        insert(additions, dirtyPath);
    }

    private void insert(Collection<String> additions, Path destination) {
        try {
            FileWriter writer = new FileWriter(destination.toFile(), true);
            for (String structureIdentifier : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = structureIdentifier + "\n";
                writer.append(update);
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteKnown(Collection<String> removals) {
        delete(removals, knownPath);
    }

    @Override
    public void deleteDirty(Collection<String> removals) {
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

    private void delete(Collection<String> removals, Path destination) {
        try {
            String output = Files.lines(destination)
                    .filter(line -> removals.stream().noneMatch(line::startsWith))
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.write(destination, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
