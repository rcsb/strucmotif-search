package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A file-system-based state repository.
 */
@Service
public class StateRepositoryImpl implements StateRepository {
    private static final Logger logger = LoggerFactory.getLogger(StateRepositoryImpl.class);
    private static final String TOP_LEVEL_DELIMITER = ",";
    private final Path knownPath;
    private final Path dirtyPath;

    /**
     * Construct a state repository instance.
     * @param strucmotifConfig the config
     */
    public StateRepositoryImpl(StrucmotifConfig strucmotifConfig) {
        Path rootPath = Paths.get(strucmotifConfig.getRootPath());
        this.knownPath = rootPath.resolve(StrucmotifConfig.STATE_KNOWN_LIST);
        this.dirtyPath = rootPath.resolve(StrucmotifConfig.STATE_DIRTY_LIST);
    }

    @Override
    public Set<StructureInformation> selectKnown() {
        try (Stream<String> lines = Files.lines(knownPath)) {
            return lines.filter(line -> !line.isBlank())
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
        String structureIdentifier = split[0];
        int structureIndex = Integer.parseInt(split[1]);
        return new StructureInformation(structureIdentifier, structureIndex, Integer.parseInt(split[2]), Integer.parseInt(split[3]));
    }

    @Override
    public Set<String> selectDirty() {
        if (Files.exists(dirtyPath)) {
            return select(dirtyPath);
        } else {
            return Collections.emptySet();
        }
    }

    private Set<String> select(Path source) {
        try (Stream<String> lines = Files.lines(source)) {
            return lines.filter(line -> !line.isBlank())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @Override
    public void insertKnown(Set<StructureInformation> additions) {
        logger.debug("Inserting information on {} structures", additions.size());
        try (FileWriter writer = new FileWriter(knownPath.toFile(), true)) {
            for (StructureInformation addition : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = addition.structureIdentifier() + TOP_LEVEL_DELIMITER +
                        addition.structureIndex() + TOP_LEVEL_DELIMITER +
                        addition.majorRevision() + TOP_LEVEL_DELIMITER +
                        addition.minorRevision() + "\n";
                writer.append(update);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        logger.debug("Structure holdings have been updated");
    }

    @Override
    public void insertDirty(Set<String> additions) {
        logger.debug("Marking {} structures as dirty", additions.size());
        insert(additions, dirtyPath);
        logger.debug("Dirty holdings have been updated");
    }

    private void insert(Set<String> additions, Path destination) {
        try (FileWriter writer = new FileWriter(destination.toFile(), true)) {
            for (String structureIdentifier : additions) {
                // let's concat externally in case 'append' invocation from multiple threads race
                String update = structureIdentifier + "\n";
                writer.append(update);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteKnown(Set<String> removals) {
        logger.debug("Removing information on {} structures", removals.size());
        delete(removals, knownPath);
        logger.debug("Structure holdings have been updated");
    }

    @Override
    public void deleteDirty(Set<String> removals) {
        logger.debug("Removing dirty state for {} structures", removals.size());
        if (Files.exists(dirtyPath)) {
            delete(removals, dirtyPath);
        } else {
            try {
                Files.createFile(dirtyPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        logger.debug("Dirty holdings have been updated");
    }

    private static void delete(Set<String> removals, Path destination) {
        if (!Files.exists(destination)) {
            return;
        }

        try (Stream<String> lines = Files.lines(destination)) {
            String output = lines.filter(line -> !removals.contains(line.split(TOP_LEVEL_DELIMITER)[0]))
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.write(destination, output.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
