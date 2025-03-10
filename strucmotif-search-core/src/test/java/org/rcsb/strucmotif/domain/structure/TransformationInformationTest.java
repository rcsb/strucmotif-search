package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.DefaultStructureReader;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

public class TransformationInformationTest {
    private static final float SINGLE_DIGIT_PRECISION = 0.1f;
    private DefaultStructureReader structureReader;

    @BeforeEach
    public void init() {
        this.structureReader = new DefaultStructureReader(new DefaultResidueTypeResolver(new StrucmotifConfig()));
    }

    @Test
    void when1m4x_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1m4x"));

        int firstResidueChainA = 0;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainA));
        assertEquals(1, structure.getLabelSeqId(firstResidueChainA));
        assertEquals("A", structure.getLabelAsymId(firstResidueChainA));
        assertEquals("1x61" , structure.getTransformationIdentifier(firstResidueChainA));
        assertEquals(ResidueType.THREONINE, structure.getResidueType(firstResidueChainA));
        assertEquals(firstResidueChainA, structure.getResidueIndex("A", "1x61", 1));

        int lastResidueChainA = 412;
        assertEquals("1", structure.getAssemblyIdentifier(lastResidueChainA));
        assertEquals(413, structure.getLabelSeqId(lastResidueChainA));
        assertEquals("A", structure.getLabelAsymId(lastResidueChainA));
        assertEquals("1x61" , structure.getTransformationIdentifier(lastResidueChainA));
        assertEquals(ResidueType.ASPARAGINE, structure.getResidueType(lastResidueChainA));
        assertEquals(lastResidueChainA, structure.getResidueIndex("A", "1x61", 413));

        int firstResidueChainB = 413;
        assertEquals("1", structure.getAssemblyIdentifier(firstResidueChainB));
        assertEquals(1, structure.getLabelSeqId(firstResidueChainB));
        assertEquals("B", structure.getLabelAsymId(firstResidueChainB));
        assertEquals("1x61" , structure.getTransformationIdentifier(firstResidueChainB));
        assertEquals(ResidueType.THREONINE, structure.getResidueType(firstResidueChainB));
        assertEquals(firstResidueChainB, structure.getResidueIndex("B", "1x61", 1));
    }

    @Test
    void when1qd6_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1qd6"));
        assertThrows(NoSuchElementException.class, () -> structure.getResidueIndex("1", "D", "1", 77), "Chain 'D' isn't part of assembly 1");
        assertEquals(346, structure.getResidueIndex("2", "D", "1", 77));
        assertEquals(854, structure.getResidueIndex("3", "D", "1", 77));
        assertThrows(NoSuchElementException.class, () -> structure.getResidueIndex("4", "D", "1", 77), "There's no assembly 4");
    }

    @Test
    void when1m4x_thenResidueTransformCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1m4x"));

        // assert transform are composed correctly
        float[] t1x61 = structure.getTransformation("1x61");
        assertArrayEquals(new float[] { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f }, t1x61);
        float[] t1x62 = structure.getTransformation("1x62");
        assertArrayEquals(new float[] { 0.999773f, 0.014247f, -0.015805f, -49.486f, -0.014842f, 0.999161f, -0.038166f, 14.993f, 0.015248f, 0.038393f, 0.999147f, 32.902f, 0.0f, 0.0f, 0.0f, 1.0f }, t1x62);
        float[] t2x61 = structure.getTransformation("2x61");
        assertArrayEquals(new float[] { 0.809017f, 0.309017f, 0.5f, 0.0f, 0.309017f, 0.5f, -0.809017f, 0.0f, -0.5f, 0.809017f, 0.309017f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f }, t2x61);

        int i1 = structure.getResidueIndex("A", "1x61", 1);
        assertEquals(0, i1);
        assertArrayEquals(new float[] { 530.3f, 578.10004f, 67.8f }, structure.manifestResidue(i1).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i2 = structure.getResidueIndex("A", "1x61", 413);
        assertEquals(412, i2);
        assertArrayEquals(new float[] { 547.5f, 586.7f, 24.300001f }, structure.manifestResidue(i2).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i3 = structure.getResidueIndex("B", "1x61", 1);
        assertEquals(413, i3);
        assertArrayEquals(new float[] { 527.60004f, 583.60004f, 44.2f }, structure.manifestResidue(i3).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i4 = structure.getResidueIndex("B", "1x61", 413);
        assertEquals(825, i4);
        assertArrayEquals(new float[] { 559.0f, 571.5f, 77.9f }, structure.manifestResidue(i4).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i5 = structure.getResidueIndex("C", "1x61", 1);
        assertEquals(826, i5);
        assertArrayEquals(new float[] { 547.8f, 572.3f, 51.9f }, structure.manifestResidue(i5).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i6 = structure.getResidueIndex("C", "1x61", 413);
        assertEquals(1238, i6);
        assertArrayEquals(new float[] { 510.0f, 597.7f, 65.6f }, structure.manifestResidue(i6).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i7 = structure.getResidueIndex("A", "1x62", 1);
        assertEquals(1239, i7);
        assertArrayEquals(new float[] { 487.85825f, 582.1496f, 130.92517f }, structure.manifestResidue(i7).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i8 = structure.getResidueIndex("A", "1x62", 413);
        assertEquals(1239 + 412, i8);
        assertArrayEquals(new float[] { 505.8644f, 592.14734f, 88.054726f }, structure.manifestResidue(i8).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i9 = structure.getResidueIndex("B", "1x62", 1);
        assertEquals(1239 + 413, i9);
        assertArrayEquals(new float[] { 485.61026f, 588.5858f, 107.5153f }, structure.manifestResidue(i9).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i10 = structure.getResidueIndex("B", "1x62", 413);
        assertEquals(1239 + 825, i10);
        assertArrayEquals(new float[] { 516.29803f, 574.74365f, 141.20078f }, structure.manifestResidue(i10).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i11 = structure.getResidueIndex("C", "1x62", 1);
        assertEquals(1239 + 826, i11);
        assertArrayEquals(new float[] { 505.52298f, 576.70154f, 115.0829f }, structure.manifestResidue(i11).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);

        int i12 = structure.getResidueIndex("C", "1x62", 413);
        assertEquals(1239 + 1238, i12);
        assertArrayEquals(new float[] { 467.8768f, 602.11847f, 129.17001f }, structure.manifestResidue(i12).get(LabelAtomId.CA), SINGLE_DIGIT_PRECISION);
    }

    @Test
    void whenFirstInterwovenAssembly_thenResiduesMapCorrectly() {
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
    void whenSecondInterwovenAssembly_thenResiduesMapCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));

        int firstResidueChainA = 499;
        assertEquals("2", structure.getAssemblyIdentifier(firstResidueChainA));
        assertEquals(4, structure.getLabelSeqId(firstResidueChainA));
        assertEquals("A", structure.getLabelAsymId(firstResidueChainA));
        assertEquals("1" , structure.getTransformationIdentifier(firstResidueChainA));
        assertEquals(0, structure.getResidueIndex("A", "1", 4));
        assertEquals(0, structure.getResidueIndex("1", "A", "1", 4));
        assertEquals(499, structure.getResidueIndex("2", "A", "1", 4));
        assertThrows(NoSuchElementException.class, () -> structure.getResidueIndex("3", "A", "1", 4), "Chain 'A' doesn't occur in assembly '3'");

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
    void whenThirdInterwovenAssembly_thenResiduesMapCorrectly() {
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

    @Test
    void whenSingleAssemblyNoTransforms_thenCoordinatesUnchanged() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1dsd"));

        float[] transformation = structure.getTransformation("1");
        assertTrue(transformation[0] == 1.0f && transformation[5] == 1.0f && transformation[10] == 1.0f, "Expected identity matrix");
        assertArrayEquals(new float[] { 6.8f, 0.4f, -3.4f }, structure.manifestResidue(16).get(LabelAtomId.CA));
    }
}
