package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.cif.CifIO;
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
import java.util.Comparator;

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
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1exr";
        InputStream inputStream = Helpers.getOriginalBcif(id);
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(1026, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWritingAF_A0A0R0FWM3_F1_thenContractHonored() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(ResidueQualityStrategy.NONE);
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "AF-A0A0R0FWM3-F1";
        InputStream inputStream = Helpers.getResource("orig/" + id + "-model_v1.cif");
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile);
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
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "1exr";
        InputStream inputStream = Helpers.getOriginalBcif(id);
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(1004, atomSite.getRowCount(), "Atom count doesn't match");
    }

    @Test
    void whenWritingAF_A0A0R0FWM3_F1_FilteredByMetric_thenAtomCountDecreased() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(tempDirectory.toFile().getAbsolutePath());
        strucmotifConfig.setResidueQualityStrategy(ResidueQualityStrategy.QA_METRIC_LOCAL_ABOVE_CUTOFF);
        strucmotifConfig.setResidueQualityCutoff(70);
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        StructureWriter structureWriter = new DefaultStructureWriter(residueTypeResolver, strucmotifConfig);

        String id = "AF-A0A0R0FWM3-F1";
        InputStream inputStream = Helpers.getResource("orig/" + id + "-model_v1.cif");
        MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);

        Path outputPath = tempDirectory.resolve(id + ".bcif.gz");
        byte[] bytes = structureWriter.write(mmCifFile);
        Files.write(outputPath, bytes);

        MmCifFile read = CifIO.readFromPath(outputPath).as(StandardSchemata.MMCIF);
        AtomSite atomSite = read.getFirstBlock().getAtomSite();
        // this number ensures: polymer-only, no alt-locs, no ambiguous atom names
        assertEquals(39, atomSite.getRowCount(), "Atom count doesn't match");
    }

    private void delete(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(file -> {
                    logger.info("Deleting {}", file.getAbsolutePath());
                    file.delete();
                });
    }
}