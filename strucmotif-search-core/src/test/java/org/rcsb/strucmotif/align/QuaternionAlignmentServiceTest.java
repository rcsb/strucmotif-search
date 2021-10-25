package org.rcsb.strucmotif.align;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class QuaternionAlignmentServiceTest {
    private AlignmentService alignmentService;
    private StructureReader structureReader;

    @BeforeEach
    public void init() {
        alignmentService = new QuaternionAlignmentService();
        structureReader = new StructureReaderImpl();
    }

    @Test
    public void whenOriginalExample_thenRmsdMatches() {
        // https://theobald.brandeis.edu/qcp/main.c
        Structure structure1 = new Structure("1tst",
                new String[] {"A" },
                new int[] { 0, 6 },
                new int[]  { 1, 2, 3, 4, 5, 6, 7 },
                new int[] { 0, 1, 2, 3, 4, 5, 6 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{-2.803, 0.893, 1.368, -1.651, -0.440, 2.551, 0.105}),
                Helpers.convertCoordsToShort(new double[]{-15.373, -16.062, -12.371, -12.153, -15.218, -13.273, -11.330}),
                Helpers.convertCoordsToShort(new double[]{24.556, 25.147, 25.885, 28.177, 30.068, 31.372, 33.567 }),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });

        Structure structure2 = new Structure("2tst",
                new String[] {"A" },
                new int[] { 0, 6 },
                new int[]  { 1, 2, 3, 4, 5, 6, 7 },
                new int[] { 0, 1, 2, 3, 4, 5, 6 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{-14.739, -12.473, -14.802, -17.782, -16.124, -15.029, -18.577}),
                Helpers.convertCoordsToShort(new double[]{-18.673, -15.810, -13.307, -14.852, -14.617, -11.037, -10.001}),
                Helpers.convertCoordsToShort(new double[]{15.040, 16.074, 14.408, 16.171, 19.584f, 18.902, 17.996}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });

        List<Integer> indices = List.of(0, 1, 2, 3, 4, 5, 6);
        List<Map<LabelAtomId, float[]>> residues1 = indices.stream().map(structure1::manifestResidue).collect(Collectors.toList());
        List<Map<LabelAtomId, float[]>> residues2 = indices.stream().map(structure2::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignmentResult = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(0.719106, alignmentResult.getRootMeanSquareDeviation(), Helpers.RELAXED_DELTA);
    }

    @Test
    public void whenSelfAlign_thenRmsdIsZeroAndTransformationIsIdentity() {
        Structure structure = new Structure("1tst",
                new String[] {"A" },
                new int[] { 0, 2 },
                new int[]  { 1, 2, 3 },
                new int[] { 0, 1, 2 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{6.994, 9.429, 5.547}),
                Helpers.convertCoordsToShort(new double[]{8.354, 7.479, 0.158}),
                Helpers.convertCoordsToShort(new double[]{42.405, 48.266, 42.050}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<LabelAtomId, float[]>> residues = indices.stream().map(structure::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment11 = alignmentService.align(residues, residues, AtomPairingScheme.ALL);

        double rmsd11 = alignment11.getRootMeanSquareDeviation();
        float[] transformation11 = alignment11.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { 1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1 }, transformation11, Helpers.RELAXED_DELTA);
        assertEquals(0.0, rmsd11, Helpers.RELAXED_DELTA);
    }

    @Test
    public void whenAlign1And2_thenRmsdAndTransformationMatches() {
        Structure structure1 = new Structure("1tst",
                new String[] {"A" },
                new int[] { 0, 2 },
                new int[]  { 1, 2, 3 },
                new int[] { 0, 1, 2 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{6.994, 9.429, 5.547}),
                Helpers.convertCoordsToShort(new double[]{8.354, 7.479, 0.158}),
                Helpers.convertCoordsToShort(new double[]{42.405, 48.266, 42.050}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });
        Structure structure2 = new Structure("2tst",
                new String[] {"A" },
                new int[] { 0, 2 },
                new int[]  { 1, 2, 3 },
                new int[] { 0, 1, 2 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{3.908, 4.588, 12.080}),
                Helpers.convertCoordsToShort(new double[]{12.066, 6.531, 12.645}),
                Helpers.convertCoordsToShort(new double[]{-6.159, -9.119, -7.073}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<LabelAtomId, float[]>> residues1 = indices.stream().map(structure1::manifestResidue).collect(Collectors.toList());
        List<Map<LabelAtomId, float[]>> residues2 = indices.stream().map(structure2::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment12 = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);

        double rmsd12 = alignment12.getRootMeanSquareDeviation();
        float[] transformation12 = alignment12.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { -0.054316826f, -0.7567086f, 0.65149194f, 20.407219f,
                -0.9968595f, 0.07874857f, 0.008355401f, 11.455171f,
                -0.057626665f, -0.6489921f, -0.75860953f, 45.747604f,
                0.0f, 0.0f, 0.0f, 1.0f }, transformation12, Helpers.RELAXED_DELTA);
        assertEquals(0.19986, rmsd12, Helpers.RELAXED_DELTA);
    }

    @Test
    public void whenAlign3And4_thenRmsdAndTransformationMatches() {
        Structure structure3 = new Structure("1tst",
                new String[] {"A" },
                new int[] { 0, 2 },
                new int[]  { 1, 2, 3 },
                new int[] { 0, 1, 2 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{5.055, 7.320, 6.021}),
                Helpers.convertCoordsToShort(new double[]{74.865, 76.960, 74.874}),
                Helpers.convertCoordsToShort(new double[]{22.585, 20.325, 17.385}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });
        Structure structure4 = new Structure("2tst",
                new String[] {"A" },
                new int[] { 0, 2 },
                new int[]  { 1, 2, 3 },
                new int[] { 0, 1, 2 },
                Helpers.convertEnumToByte(ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE),
                Helpers.convertEnumToByte(LabelAtomId.CA, LabelAtomId.CA, LabelAtomId.CA),
                Helpers.convertCoordsToShort(new double[]{5.055, 7.321, 6.020}),
                Helpers.convertCoordsToShort(new double[]{74.864, 76.962, 74.873}),
                Helpers.convertCoordsToShort(new double[]{22.583, 20.326, 17.386}),
                new String[] { "1" },
                new String[][] { { "A" } },
                new String[] { "1" },
                new Transformation[] { Transformation.IDENTITY_TRANSFORMATION });

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<LabelAtomId, float[]>> residues1 = indices.stream().map(structure3::manifestResidue).collect(Collectors.toList());
        List<Map<LabelAtomId, float[]>> residues2 = indices.stream().map(structure4::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment34 = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);

        double rmsd34 = alignment34.getRootMeanSquareDeviation();
        float[] transformation34 = alignment34.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { 0.9999999693395323f, 2.476156855096834E-4f, 2.7214443547168843E-6f, -0.018765917915588126f,
                -2.4761583288007705E-4f, 0.9999999678697555f, 5.428524155062786E-5f, 4.2976537221761646E-4f,
                -2.70800238995339E-6f, -5.428591375893077E-5f, 0.9999999985228533f, 0.004118822613254025f,
                0, 0, 0, 1 }, transformation34, Helpers.RELAXED_DELTA);
        assertEquals(0.0021, rmsd34, Helpers.RELAXED_DELTA);
    }

    @Test
    public void whenAminopeptidaseExampleWithAllAtoms_thenRmsdInflated() {
        Structure structure1 = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        List<Map<LabelAtomId, float[]>> residues1 = Stream.of("A-250", "A-255", "A-273", "A-332", "A-334")
                .map(id -> id.split("-"))
                .map(split -> structure1.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure1::manifestResidue)
                .collect(Collectors.toList());
        Structure structure2 = structureReader.readFromInputStream(getOriginalBcif("3pei"));
        List<Map<LabelAtomId, float[]>> residues2 = Stream.of("A-251", "A-256", "A-274", "A-333", "A-335")
                .map(id -> id.split("-"))
                .map(split -> structure2.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure2::manifestResidue)
                .collect(Collectors.toList());

        AlignmentResult result = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(3.167, result.getRootMeanSquareDeviation(), Helpers.DELTA);
    }

    @Test
    public void whenAminopeptidaseExampleWithNonAmbiguous_thenRmsdMatches() {
        Set<LabelAtomId> ambiguous = Set.of(LabelAtomId.OD1, LabelAtomId.OD2, LabelAtomId.OE1, LabelAtomId.OE2);
        Structure structure1 = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        List<Map<LabelAtomId, float[]>> residues1 = Stream.of("A-250", "A-255", "A-273", "A-332", "A-334")
                .map(id -> id.split("-"))
                .map(split -> structure1.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure1::manifestResidue)
                // manually filter away ambiguous atoms of GLU
                .map(map -> map.entrySet().stream().filter(entry -> !ambiguous.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
        Structure structure2 = structureReader.readFromInputStream(getOriginalBcif("3pei"));
        List<Map<LabelAtomId, float[]>> residues2 = Stream.of("A-251", "A-256", "A-274", "A-333", "A-335")
                .map(id -> id.split("-"))
                .map(split -> structure2.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure2::manifestResidue)
                // manually filter away ambiguous atoms of GLU
                .map(map -> map.entrySet().stream().filter(entry -> !ambiguous.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());

        AlignmentResult result = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(2.211, result.getRootMeanSquareDeviation(), Helpers.RELAXED_DELTA);
    }
}
