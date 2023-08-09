package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.DefaultStructureReader;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.all;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.chainsInContact;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.deposited;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.residuesInContact;

public class ResidueGraphOptionTest {
    private static final float TEST_DISTANCE_CUTOFF = 20;
    private DefaultStructureReader structureReader;
    private StrucmotifConfig strucmotifConfig;

    @BeforeEach
    public void init() {
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setDistanceCutoff(TEST_DISTANCE_CUTOFF);
        structureReader = new DefaultStructureReader(new DefaultResidueTypeResolver(strucmotifConfig));
    }

    @Test
    void when1lapDeposited_thenNoTransformedChains() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, deposited());

        boolean noTransforms = residueGraph.residuePairOccurrencesSequential()
                .noneMatch(o -> !structure.getTransformationIdentifier(o.getResidueIndex1()).equals("1") || !structure.getTransformationIdentifier(o.getResidueIndex2()).equals("1"));
        assertTrue(noTransforms);
        assertEquals(481, residueGraph.getResidueCount());
        assertEquals(481, residueGraph.residuePairOccurrencesSequential().flatMapToInt(o -> IntStream.of(o.getResidueIndex1(), o.getResidueIndex2())).distinct().count());
        assertEquals(28342, residueGraph.getPairingCount());
    }

    @Test
    void when1lapResiduesInContact_thenSubsetReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, residuesInContact());

        boolean noTransforms = residueGraph.residuePairOccurrencesSequential()
                .noneMatch(o -> !structure.getTransformationIdentifier(o.getResidueIndex1()).equals("1") || !structure.getTransformationIdentifier(o.getResidueIndex2()).equals("1"));
        assertFalse(noTransforms);
        assertTrue(481 < residueGraph.getResidueCount());
        assertTrue(2886 > residueGraph.residuePairOccurrencesSequential().flatMapToInt(o -> IntStream.of(o.getResidueIndex1(), o.getResidueIndex2())).distinct().count());
        assertEquals(39554, residueGraph.getPairingCount());
    }

    @Test
    void when1lapChainsInContact_thenEverythingReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        boolean noTransforms = residueGraph.residuePairOccurrencesSequential()
                .noneMatch(o -> !structure.getTransformationIdentifier(o.getResidueIndex1()).equals("1") || !structure.getTransformationIdentifier(o.getResidueIndex2()).equals("1"));
        assertFalse(noTransforms);
        assertEquals(2886, residueGraph.getResidueCount());
        assertEquals(2886, residueGraph.residuePairOccurrencesSequential().flatMapToInt(o -> IntStream.of(o.getResidueIndex1(), o.getResidueIndex2())).distinct().count());
        assertEquals(203688, residueGraph.getPairingCount());
    }

    @Test
    void when1lapAll_thenEverythingReturned() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, all());

        boolean noTransforms = residueGraph.residuePairOccurrencesSequential()
                .noneMatch(o -> !structure.getTransformationIdentifier(o.getResidueIndex1()).equals("1") || !structure.getTransformationIdentifier(o.getResidueIndex2()).equals("1"));
        assertFalse(noTransforms);
        assertEquals(2886, residueGraph.getResidueCount());
        assertEquals(2886, residueGraph.residuePairOccurrencesSequential().flatMapToInt(o -> IntStream.of(o.getResidueIndex1(), o.getResidueIndex2())).distinct().count());
        assertEquals(203688, residueGraph.getPairingCount());
    }
}
