package org.rcsb.strucmotif.update;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.core.ThreadPoolImpl;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureDataProviderImpl;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;
import org.rcsb.strucmotif.io.StructureWriter;
import org.rcsb.strucmotif.io.StructureWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(UpdateIntegrationTest.class);
    private static final List<TestCases> UPDATE_1 = List.of(TestCases.PDB_2RLL, TestCases.PDB_3ULN, TestCases.PDB_3UM4);
    private static final List<TestCases> UPDATE_2 = List.of(TestCases.PDB_4TUT, TestCases.PDB_5XES, TestCases.PDB_6FCE);
    private static final List<TestCases> UPDATE_3 = List.of(TestCases.AF_A0A0R0FWM3, TestCases.AF_Q8SY76);
    private static final List<TestCases> UPDATE_OVERLAP = List.of(TestCases.PDB_2RLL, TestCases.PDB_4TUT);
    private MotifSearchConfig config;
    private Path path;
    private Path renumberedPath;
    private StateRepository state;
    private StructureDataProvider data;
    private InvertedIndex index;
    private MotifSearchUpdate update;

    @BeforeEach
    public void setup() throws IOException {
        this.config = new MotifSearchConfig();
        config.setUndefinedAssemblies(true);
        this.path = Files.createTempDirectory("strucmotif-update-tests-");
        Path indexPath = path.resolve("index");
        Files.createDirectories(indexPath);
        this.renumberedPath = path.resolve("renumbered");
        Files.createDirectories(renumberedPath);

        config.setRootPath(path.toFile().getAbsolutePath());
        this.state = new StateRepositoryImpl(config);
        StructureReader reader = new StructureReaderImpl();
        StructureWriter writer = new StructureWriterImpl(config);
        this.data = new StructureDataProviderImpl(reader, writer, config);
        this.index = new InvertedIndexImpl(config);

        init();
    }

    /**
     * Some operations may need to rerun to update application state.
     */
    private void init() {
        ThreadPool pool = new ThreadPoolImpl(config);
        StructureIndexProvider keys = new StructureIndexProviderImpl(state);
        this.update = new MotifSearchUpdate(state, data, index, config, pool, keys) {
            @Override
            protected InputStream handleInputStream(UpdateItem item, Context context) {
                return TestCases.getInputStream(item.getStructureIdentifier());
            }
        };
    }

    @AfterEach
    public void teardown() throws IOException {
        logger.info("Deleting tmp dir at {}", path);
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private String[] toArgs(Operation operation, List<TestCases> cases) {
        return Stream.concat(Stream.of(operation).map(Operation::name), cases.stream().map(TestCases::getExpression))
                .toArray(String[]::new);
    }

    @Test
    public void whenAdding_thenCountsIncrease() throws Exception {
        assertStateCounts(0, 0, 0);

        update.run(toArgs(Operation.ADD, UPDATE_1));
        init();
        assertStateCounts(3, 3, 3);

        update.run(toArgs(Operation.ADD, UPDATE_1));
        init();
        assertStateCounts(3, 3, 3);

        update.run(toArgs(Operation.ADD, UPDATE_OVERLAP));
        init();
        assertStateCounts(4, 4, 4);

        update.run(toArgs(Operation.ADD, UPDATE_2));
        init();
        assertStateCounts(6, 6, 6);

        update.run(toArgs(Operation.ADD, UPDATE_3));
        init();
        assertStateCounts(8, 8, 8);
    }

    private void assertStateCounts(int stateCount, int structureCount, int indexCount) throws IOException {
        assertEquals(stateCount, state.reportKnownKeys().size(), stateCount == 0 ? "State should be empty" : "State count don't match");
        assertEquals(structureCount, Files.list(renumberedPath).count(), structureCount == 0 ? "Structure data dir should be empty" : "Structure count don't match");
        assertEquals(indexCount, index.reportKnownKeys().size(), indexCount == 0 ? "Index should be empty" : "Index count don't match");
    }

    @Test
    public void whenRemoving_thenNoOperation() throws Exception {
        assertStateCounts(0, 0, 0);

        update.run(toArgs(Operation.REMOVE, UPDATE_1));
        init();
        assertStateCounts(0, 0, 0);

        update.run(toArgs(Operation.REMOVE, UPDATE_2));
        init();
        assertStateCounts(0, 0, 0);

        update.run(toArgs(Operation.REMOVE, UPDATE_3));
        init();
        assertStateCounts(0, 0, 0);
    }

    @Test
    public void whenAddingAndRemoving_thenCountsIncrease() throws Exception {
        assertStateCounts(0, 0, 0);

        update.run(toArgs(Operation.ADD, UPDATE_1));
        init();
        assertStateCounts(3, 3, 3);

        update.run(toArgs(Operation.REMOVE, UPDATE_OVERLAP));
        init();
        assertStateCounts(2, 2, 2);

        update.run(toArgs(Operation.ADD, UPDATE_2));
        init();
        assertStateCounts(5, 5, 5);

        update.run(toArgs(Operation.ADD, UPDATE_3));
        init();
        assertStateCounts(7, 7, 7);

        update.run(toArgs(Operation.REMOVE, UPDATE_OVERLAP));
        init();
        assertStateCounts(6, 6, 6);
    }
}