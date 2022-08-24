package org.rcsb.strucmotif.update;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.InvertedIndexBackend;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.core.ThreadPoolImpl;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.ResidueTypeResolver;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
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

class UpdateIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(UpdateIntegrationTest.class);
    private static final List<TestCases> UPDATE_1 = List.of(TestCases.PDB_2RLL, TestCases.PDB_3ULN, TestCases.PDB_3UM4);
    private static final List<TestCases> UPDATE_2 = List.of(TestCases.PDB_4TUT, TestCases.PDB_5XES, TestCases.PDB_6FCE);
    private static final List<TestCases> UPDATE_3 = List.of(TestCases.AF_A0A0R0FWM3, TestCases.AF_Q8SY76);
    private static final List<TestCases> UPDATE_OVERLAP = List.of(TestCases.PDB_2RLL, TestCases.PDB_4TUT);
    private StrucmotifConfig strucmotifConfig;
    private Path path;
    private StateRepository state;
    private StructureDataProviderImpl data;
    private InvertedIndexImpl index;
    private StrucmotifUpdate update;

    @BeforeEach
    public void setup() throws IOException {
        this.strucmotifConfig = new StrucmotifConfig();
        // ensure merging and side-by-side temporary files happen
        strucmotifConfig.setUpdateChunkSize(1);
        strucmotifConfig.setCommitInterval(1);
        strucmotifConfig.setInvertedIndexBackend(InvertedIndexBackend.COLFER);
        this.path = Files.createTempDirectory("strucmotif-update-tests-");
        Files.createFile(path.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.DATA_EXT));
        Files.createFile(path.resolve(StrucmotifConfig.INDEX + StrucmotifConfig.INDEX_EXT));
        Files.createFile(path.resolve(StrucmotifConfig.RENUMBERED + StrucmotifConfig.DATA_EXT));
        Files.createFile(path.resolve(StrucmotifConfig.RENUMBERED + StrucmotifConfig.INDEX_EXT));

        strucmotifConfig.setRootPath(path.toFile().getAbsolutePath());
        this.state = new StateRepositoryImpl(strucmotifConfig);
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureReader reader = new StructureReaderImpl(residueTypeResolver);
        StructureWriter writer = new StructureWriterImpl(residueTypeResolver, strucmotifConfig);
        ThreadPool threadPool = new ThreadPoolImpl(strucmotifConfig);
        this.data = new StructureDataProviderImpl(reader, writer, threadPool, strucmotifConfig);
        this.index = new InvertedIndexImpl(threadPool, strucmotifConfig);

        init();
    }

    /**
     * Some operations may need to rerun to update application state.
     */
    private void init() throws IOException {
        ThreadPool pool = new ThreadPoolImpl(strucmotifConfig);
        StructureIndexProvider keys = new StructureIndexProviderImpl(state);
        this.update = new StrucmotifUpdate(state, data, index, strucmotifConfig, pool, keys) {
            @Override
            protected InputStream handleInputStream(UpdateItem item, Context context) {
                return TestCases.getInputStream(item.getStructureIdentifier());
            }
        };
        this.index.setUp();
        this.data.setUp();
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
    void whenAdding_thenCountsIncrease() throws Exception {
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

    private void assertStateCounts(int stateCount, int structureCount, int indexCount) {
        assertEquals(stateCount, state.reportKnownKeys().size(), stateCount == 0 ? "State should be empty" : "State counts don't match");
        assertEquals(structureCount, data.reportKnownFiles().size(), structureCount == 0 ? "Structure data dir should be empty" : "Structure counts don't match");
        assertEquals(indexCount, index.reportKnownKeys().size(), indexCount == 0 ? "Index should be empty" : "Index counts don't match");
    }

    @Test
    void whenRemoving_thenNoOperation() throws Exception {
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
    void whenAddingAndRemoving_thenCountsIncrease() throws Exception {
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
