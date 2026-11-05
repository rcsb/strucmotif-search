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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
     * Two alanines whose B-factor and local QA metric disagree: residue 1 has B-factor 90 and metric 10, residue 2 has
     * B-factor 10 and metric 90.
     */
    private static final String CONFLICTING_QUALITY_CIF = """
            data_TEST
            loop_
            _atom_site.group_PDB
            _atom_site.id
            _atom_site.type_symbol
            _atom_site.label_atom_id
            _atom_site.label_alt_id
            _atom_site.label_comp_id
            _atom_site.label_asym_id
            _atom_site.label_seq_id
            _atom_site.Cartn_x
            _atom_site.Cartn_y
            _atom_site.Cartn_z
            _atom_site.B_iso_or_equiv
            _atom_site.pdbx_PDB_model_num
            ATOM 1  N N  . ALA A 1 0.000 0.000 0.000 90.00 1
            ATOM 2  C CA . ALA A 1 1.458 0.000 0.000 90.00 1
            ATOM 3  C C  . ALA A 1 2.009 1.420 0.000 90.00 1
            ATOM 4  O O  . ALA A 1 1.251 2.390 0.000 90.00 1
            ATOM 5  C CB . ALA A 1 1.988 -0.773 -1.199 90.00 1
            ATOM 6  N N  . ALA A 2 3.332 1.536 0.000 10.00 1
            ATOM 7  C CA . ALA A 2 3.970 2.846 0.000 10.00 1
            ATOM 8  C C  . ALA A 2 5.486 2.705 0.000 10.00 1
            ATOM 9  O O  . ALA A 2 6.009 1.593 0.000 10.00 1
            ATOM 10 C CB . ALA A 2 3.517 3.630 -1.226 10.00 1
            #
            loop_
            _ma_qa_metric_local.ordinal_id
            _ma_qa_metric_local.model_id
            _ma_qa_metric_local.label_asym_id
            _ma_qa_metric_local.label_seq_id
            _ma_qa_metric_local.label_comp_id
            _ma_qa_metric_local.metric_id
            _ma_qa_metric_local.metric_value
            1 1 A 1 ALA 2 10.00
            2 1 A 2 ALA 2 90.00
            #
            """;

    /**
     * B-factor strategies used to fall through to the QA metric filter, so residue 1 got dropped for its metric.
     */
    @Test
    void whenFilteringByBFactor_thenQaMetricIgnored() throws IOException {
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1 }, writeConflictingQuality(ResidueQualityStrategy.BFACTOR_ABOVE_CUTOFF),
                "Only residue 1 has a B-factor above the cutoff");
    }

    @Test
    void whenFilteringByQaMetric_thenBFactorIgnored() throws IOException {
        assertArrayEquals(new int[] { 2, 2, 2, 2, 2 }, writeConflictingQuality(ResidueQualityStrategy.QA_METRIC_LOCAL_ABOVE_CUTOFF),
                "Only residue 2 has a local QA metric above the cutoff");
    }

    /**
     * Write {@link #CONFLICTING_QUALITY_CIF} with a cutoff of 70.
     * @return label_seq_id of all written atoms
     */
    private int[] writeConflictingQuality(ResidueQualityStrategy residueQualityStrategy) throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(residueQualityStrategy);
        strucmotifConfig.setResidueQualityCutoff(70);
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        InputStream inputStream = new ByteArrayInputStream(CONFLICTING_QUALITY_CIF.getBytes(StandardCharsets.UTF_8));
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        byte[] bytes = structureWriter.write(mmCifFile, 1);
        assertNotNull(bytes, "No residue passed the " + residueQualityStrategy + " filter");
        Path outputPath = tempDirectory.resolve(residueQualityStrategy + ".bcif.gz");
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        return read.getFirstBlock().getAtomSite().getLabelSeqId().getArray();
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