package org.rcsb.strucmotif.wip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

public class TransformationInformationTest {
    private DefaultStructureReader structureReader;

    @BeforeEach
    public void init() {
        this.structureReader = new DefaultStructureReader(new ResidueTypeResolverImpl(new StrucmotifConfig()));
    }

    @Test
    void when1m4x_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1m4x"));
    }

    @Test
    void whenFirstInterleavingAssemblies_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));

        int firstResidueChainA = 0;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainA));
        assertEquals(4, structure.getLabelSeqId(firstResidueChainA));
        assertEquals("A", structure.getLabelAsymId(firstResidueChainA));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainA));
        assertEquals(firstResidueChainA, structure.getResidueIndex("A", "1", 4));

        int lastResidueChainA = 241;
        assertEquals("1", structure.getAssemblyIdentifier(lastResidueChainA));
        assertEquals(248, structure.getLabelSeqId(lastResidueChainA));
        assertEquals("A", structure.getLabelAsymId(lastResidueChainA));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainA));
        assertEquals(lastResidueChainA, structure.getResidueIndex("A", "1", 248));

        int firstResidueChainB = 242;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainB));
        assertEquals(3, structure.getLabelSeqId(firstResidueChainB));
        assertEquals("B", structure.getLabelAsymId(firstResidueChainB));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainB));
        assertEquals(firstResidueChainB, structure.getResidueIndex("B", "1", 3));

        int lastResidueChainB = 478;
        assertEquals("1", structure.getAssemblyIdentifier(lastResidueChainB));
        assertEquals(249, structure.getLabelSeqId(lastResidueChainB));
        assertEquals("B", structure.getLabelAsymId(lastResidueChainB));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainB));
        assertEquals(lastResidueChainB, structure.getResidueIndex("B", "1", 249));

        int firstResidueChainC = 479;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainC));
        assertEquals(3, structure.getLabelSeqId(firstResidueChainC));
        assertEquals("C", structure.getLabelAsymId(firstResidueChainC));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainC));
        assertEquals(firstResidueChainC, structure.getResidueIndex("C", "1", 3));

        int lastResidueChainC = 487;
        assertEquals("1", structure.getAssemblyIdentifier(lastResidueChainC));
        assertEquals(11, structure.getLabelSeqId(lastResidueChainC));
        assertEquals("C", structure.getLabelAsymId(lastResidueChainC));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainC));
        assertEquals(lastResidueChainC, structure.getResidueIndex("C", "1", 11));

        int firstResidueChainD = 488;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainD));
        assertEquals(1, structure.getLabelSeqId(firstResidueChainD));
        assertEquals("D", structure.getLabelAsymId(firstResidueChainD));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainD));
        assertEquals(firstResidueChainD, structure.getResidueIndex("D", "1", 1));

        int lastResidueChainD = 498;
        assertEquals("1", structure.getAssemblyIdentifier(lastResidueChainD));
        assertEquals(11, structure.getLabelSeqId(lastResidueChainD));
        assertEquals("D", structure.getLabelAsymId(lastResidueChainD));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainD));
        assertEquals(lastResidueChainD, structure.getResidueIndex("D", "1", 11));
    }

    @Test
    void whenSecondInterleavingAssemblies_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));

        int firstResidueChainA = 499;
        assertEquals("2", structure.getAssemblyIdentifier(firstResidueChainA));
        assertEquals(4, structure.getLabelSeqId(firstResidueChainA));
        assertEquals("A", structure.getLabelAsymId(firstResidueChainA));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainA));
        // TODO these can't be differentiated from the first copy -- add assembly param?
        assertEquals(0, structure.getResidueIndex("A", "1", 4));

        int lastResidueChainA = 740;
        assertEquals("2", structure.getAssemblyIdentifier(lastResidueChainA));
        assertEquals(248, structure.getLabelSeqId(lastResidueChainA));
        assertEquals("A", structure.getLabelAsymId(lastResidueChainA));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainA));
        assertEquals(241, structure.getResidueIndex("A", "1", 248));

        int firstResidueChainC = 741;
        assertEquals("2", structure.getAssemblyIdentifier(firstResidueChainC));
        assertEquals(3, structure.getLabelSeqId(firstResidueChainC));
        assertEquals("C", structure.getLabelAsymId(firstResidueChainC));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainC));
        assertEquals(479, structure.getResidueIndex("C", "1", 3));

        int lastResidueChainC = 749;
        assertEquals("2", structure.getAssemblyIdentifier(lastResidueChainC));
        assertEquals(11, structure.getLabelSeqId(lastResidueChainC));
        assertEquals("C", structure.getLabelAsymId(lastResidueChainC));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainC));
        assertEquals(487, structure.getResidueIndex("C", "1", 11));
    }

    @Test
    void whenThirdInterleavingAssemblies_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));

        int firstResidueChainB = 750;
        assertEquals("3", structure.getAssemblyIdentifier(firstResidueChainB));
        assertEquals(3, structure.getLabelSeqId(firstResidueChainB));
        assertEquals("B", structure.getLabelAsymId(firstResidueChainB));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainB));
        assertEquals(242, structure.getResidueIndex("B", "1", 3));

        int lastResidueChainB = 986;
        assertEquals("3", structure.getAssemblyIdentifier(lastResidueChainB));
        assertEquals(249, structure.getLabelSeqId(lastResidueChainB));
        assertEquals("B", structure.getLabelAsymId(lastResidueChainB));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainB));
        assertEquals(478, structure.getResidueIndex("B", "1", 249));

        int firstResidueChainD = 987;
        assertEquals("3", structure.getAssemblyIdentifier(firstResidueChainD));
        assertEquals(1, structure.getLabelSeqId(firstResidueChainD));
        assertEquals("D", structure.getLabelAsymId(firstResidueChainD));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainD));
        assertEquals(488, structure.getResidueIndex("D", "1", 1));

        int lastResidueChainD = 997;
        assertEquals("3", structure.getAssemblyIdentifier(lastResidueChainD));
        assertEquals(11, structure.getLabelSeqId(lastResidueChainD));
        assertEquals("D", structure.getLabelAsymId(lastResidueChainD));
        assertEquals("1" , structure.getTransformationIdentifier(lastResidueChainD));
        assertEquals(498, structure.getResidueIndex("D", "1", 11));
    }
}
