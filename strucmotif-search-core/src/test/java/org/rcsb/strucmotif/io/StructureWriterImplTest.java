package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.Block;
import org.rcsb.cif.model.Category;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.Column;
import org.rcsb.cif.model.ValueKind;
import org.rcsb.cif.model.binary.BinaryIntColumn;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.config.ResidueQualityStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StructureWriterImplTest {
    private static final Logger logger = LoggerFactory.getLogger(StructureWriterImplTest.class);
    private Path tempDirectory;

    @BeforeEach
    public void init() throws IOException {
        this.tempDirectory = Files.createTempDirectory("strucmotif-tests");
    }

    @AfterEach
    public void teardown() throws IOException {
        delete(tempDirectory);
    }

    @Test
    void whenWriting1exr_thenContractHonored() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1exr";
        InputStream inputStream = Helpers.getOriginalBcif(id);
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 1);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(1026, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWritingAFA0A0R0FWM3F1_thenContractHonored() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(ResidueQualityStrategy.NONE);
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "AF_AFA0A0R0FWM3F1";
        InputStream inputStream = Helpers.getResource("orig/" + id + ".bcif");
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 1);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(807, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWriting1exrFilteredByBFactor_thenAtomCountDecreased() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(ResidueQualityStrategy.BFACTOR_BELOW_CUTOFF);
        strucmotifConfig.setResidueQualityCutoff(50);
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1exr";
        InputStream inputStream = Helpers.getOriginalBcif(id);
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 1);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(1004, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWritingAFA0A0R0FWM3F1_FilteredByMetric_thenAtomCountDecreased() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(ResidueQualityStrategy.QA_METRIC_LOCAL_ABOVE_CUTOFF);
        strucmotifConfig.setResidueQualityCutoff(70);
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "AF_AFA0A0R0FWM3F1";
        InputStream inputStream = Helpers.getResource("orig/" + id + ".bcif");
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 1);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(39, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWritingSpecificModel_thenCorrectModelExtracted() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1nmr";
        InputStream inputStream = Helpers.getOriginalBcif(id);
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 8);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: a single model is extracted
        assertEquals(610, atomSite.getRowCount(), "Atom count doesn't match");
        assertEquals(23.187, atomSite.getCartnX().get(0), 0.1, "Cartn_X[0] doesn't match (with reduced precision)");
    }

    /**
     * IHM entries (e.g. 9AB1, 9AAV) store an all-'.' label_alt_id as int in BinaryCIF. ciftools creates a new column
     * wrapper on every getter call, and that wrapper converts a mistyped column in full - resolving the column once per
     * row made writing quadratic in atom count (hours for 9AB1, days for 9AAV).
     */
    @Test
    void whenLabelAltIdIsIntTyped_thenColumnConvertedOnceAndContractHonored() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1exr";
        AtomicInteger conversions = new AtomicInteger();
        CifFile source = withIntTypedLabelAltId(CifIO.readFromInputStream(Helpers.getOriginalBcif(id)), conversions);
        MmCifFile mmCifFile = source.as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile, 1);
        Files.write(outputPath, bytes);

        assertEquals(1, conversions.get(), "label_alt_id must be converted once, not once per atom");
        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // same as for the String-typed original (see whenWriting1exr_thenContractHonored)
        assertEquals(1026, atomSite.getRowCount(), "Atom count doesn't match");
    }

    /**
     * Replace atom_site.label_alt_id of a BinaryCIF file by an all-'.' int column and count how often its array is
     * requested, i.e. how often ciftools has to convert it to the String[] the schema expects.
     */
    private static CifFile withIntTypedLabelAltId(CifFile source, AtomicInteger conversions) {
        Block block = source.getBlocks().get(0);
        Category atomSite = block.getCategory("atom_site");
        int rowCount = atomSite.getRowCount();
        int[] mask = new int[rowCount];
        Arrays.fill(mask, ValueKind.NOT_PRESENT.ordinal());
        Column<?> labelAltId = new BinaryIntColumn("label_alt_id", rowCount, new int[rowCount], mask) {
            @Override
            public int[] getArray() {
                conversions.incrementAndGet();
                return super.getArray();
            }
        };

        Category patchedAtomSite = new Category() {
            @Override
            public String getCategoryName() {
                return atomSite.getCategoryName();
            }

            @Override
            public int getRowCount() {
                return rowCount;
            }

            @Override
            public Column<?> getColumn(String name) {
                return "label_alt_id".equalsIgnoreCase(name) ? labelAltId : atomSite.getColumn(name);
            }

            @Override
            public List<String> getColumnNames() {
                return atomSite.getColumnNames();
            }

            @Override
            public Map<String, Column<?>> getColumns() {
                Map<String, Column<?>> columns = new LinkedHashMap<>(atomSite.getColumns());
                columns.put("label_alt_id", labelAltId);
                return columns;
            }
        };

        Block patchedBlock = new Block() {
            @Override
            public String getBlockHeader() {
                return block.getBlockHeader();
            }

            @Override
            public Category getCategory(String name) {
                return "atom_site".equalsIgnoreCase(name) ? patchedAtomSite : block.getCategory(name);
            }

            @Override
            public Map<String, Category> getCategories() {
                Map<String, Category> categories = new LinkedHashMap<>(block.getCategories());
                categories.put("atom_site", patchedAtomSite);
                return categories;
            }

            @Override
            public List<Block> getSaveFrames() {
                return block.getSaveFrames();
            }
        };

        return () -> List.of(patchedBlock);
    }

    private void delete(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        logger.info("Deleting {}", file.getAbsolutePath());
                        file.delete();
                    });
        }
    }
}