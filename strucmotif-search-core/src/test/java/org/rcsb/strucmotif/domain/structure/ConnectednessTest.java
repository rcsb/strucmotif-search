package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.DefaultStructureReader;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.StructureReader;

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
        structureReader = new DefaultStructureReader(new DefaultResidueTypeResolver(strucmotifConfig));
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
        List<Map<LabelAtomId, float[]>> res = sel.stream()
                .mapToInt(structure::getResidueIndex)
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(res, sel));
        assertTrue(residueGraph.isConnected());
    }

    @Test
    void whenStandardFullStructure_thenTrueReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.deposited());
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
        List<Map<LabelAtomId, float[]>> res = sel.stream()
                .mapToInt(structure::getResidueIndex)
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(res, sel));
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
        List<Map<LabelAtomId, float[]>> res = sel.stream()
                .mapToInt(structure::getResidueIndex)
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(res, sel));
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
        List<Map<LabelAtomId, float[]>> res = sel.stream()
                .mapToInt(structure::getResidueIndex)
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(res, sel));
        assertFalse(residueGraph.isConnected());
    }

    @Test
    void whenIn3rdAssembly_thenStillConnected() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1qd6"));
        List<LabelSelection> sel = List.of(
                new LabelSelection("C", "1", 113),
                new LabelSelection("C", "1", 115),
                new LabelSelection("C", "1", 117),
                new LabelSelection("C", "1", 118),
                new LabelSelection("C", "1", 123),
                new LabelSelection("C", "1", 127),
                new LabelSelection("D", "1", 77)
        );
        List<Map<LabelAtomId, float[]>> res = sel.stream()
                .mapToInt(structure::getResidueIndex)
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(res, sel));
        assertTrue(residueGraph.isConnected());
    }
}
