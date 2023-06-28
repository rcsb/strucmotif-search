package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class ConnectednessTest {
    private static final float TEST_DISTANCE_CUTOFF = 20;
    private StructureReader structureReader;
    private StrucmotifConfig strucmotifConfig;

    @BeforeEach
    public void init() {
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setDistanceCutoff(TEST_DISTANCE_CUTOFF);
        structureReader = new StructureReaderImpl(new ResidueTypeResolverImpl(strucmotifConfig));
    }

    @Test
    void whenGraphConnected_thenTrueReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7a3x"));
        List<LabelSelection> sel = List.of(
                new LabelSelection("A", "2", 333), // 307, ILE
                new LabelSelection("A", "2", 336), // 310, LEU
                new LabelSelection("A", "2", 343), // 317, LEU
                new LabelSelection("A", "2", 347)  // 321, MET
        );
        List<Map<LabelAtomId, float[]>> res = structure.manifestResidues(sel);
        ResidueGraph residueGraph = new ResidueGraph(structure, sel, res, strucmotifConfig);
        assertTrue(residueGraph.isConnected());
    }

    @Test
    void whenStandardFullStructure_thenTrueReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.all());
        assertTrue(residueGraph.isConnected());
    }

    @Test
    void whenGraphHasDanglingResidue_thenFalseReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7a3x"));
        List<LabelSelection> sel = List.of(
                new LabelSelection("A", "2", 333), // 307, ILE
                new LabelSelection("A", "2", 336), // 310, LEU
                new LabelSelection("A", "2", 343), // 317, LEU
                new LabelSelection("A", "2", 347), // 321, MET
                new LabelSelection("A", "2", 369)  // 373, GLU
        );
        List<Map<LabelAtomId, float[]>> res = structure.manifestResidues(sel);
        ResidueGraph residueGraph = new ResidueGraph(structure, sel, res, strucmotifConfig);
        assertFalse(residueGraph.isConnected());
    }

    @Test
    void whenGraphIsNotConnected_thenFalseReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7a3x"));
        List<LabelSelection> sel = List.of(
                new LabelSelection("A", "2", 333), // 307, ILE
                new LabelSelection("A", "2", 336), // 310, LEU
                new LabelSelection("A", "2", 343), // 317, LEU
                new LabelSelection("A", "2", 347), // 321, MET
                new LabelSelection("A", "2", 369), // 373, GLU
                new LabelSelection("A", "2", 370)  // 374, GLY
        );
        List<Map<LabelAtomId, float[]>> res = structure.manifestResidues(sel);
        ResidueGraph residueGraph = new ResidueGraph(structure, sel, res, strucmotifConfig);
        assertFalse(residueGraph.isConnected());
    }

    @Test
    void whenNoEdges_thenReportFalse() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1w27"));
        List<LabelSelection> sel = List.of(
                new LabelSelection("A", "1", 110),
                new LabelSelection("A", "1", 349),
                new LabelSelection("A", "1", 398)
        );
        List<Map<LabelAtomId, float[]>> res = structure.manifestResidues(sel);
        ResidueGraph residueGraph = new ResidueGraph(structure, sel, res, strucmotifConfig);
        residueGraph.residuePairOccurrencesSequential().forEach(System.out::println);
        assertFalse(residueGraph.isConnected());
    }
}
