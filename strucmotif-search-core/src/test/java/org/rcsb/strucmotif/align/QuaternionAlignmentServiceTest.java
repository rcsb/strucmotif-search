package org.rcsb.strucmotif.align;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
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
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2,
                new LabelSelection.SparseLabelSelection("A", 4), 3,
                new LabelSelection.SparseLabelSelection("A", 5), 4,
                new LabelSelection.SparseLabelSelection("A", 6), 5,
                new LabelSelection.SparseLabelSelection("A", 7), 6),
                new int[] { 0, 1, 2, 3, 4, 5, 6 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA", "CA", "CA", "CA", "CA" },
                new float[]{-2.803f, 0.893f, 1.368f, -1.651f, -0.440f, 2.551f, 0.105f},
                new float[]{-15.373f, -16.062f, -12.371f, -12.153f, -15.218f, -13.273f, -11.330f},
                new float[]{24.556f, 25.147f, 25.885f, 28.177f, 30.068f, 31.372f, 33.567f },
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));

        Structure structure2 = new Structure("2tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2,
                new LabelSelection.SparseLabelSelection("A", 4), 3,
                new LabelSelection.SparseLabelSelection("A", 5), 4,
                new LabelSelection.SparseLabelSelection("A", 6), 5,
                new LabelSelection.SparseLabelSelection("A", 7), 6),
                new int[] { 0, 1, 2, 3, 4, 5, 6 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA", "CA", "CA", "CA", "CA" },
                new float[]{-14.739f, -12.473f, -14.802f, -17.782f, -16.124f, -15.029f, -18.577f},
                new float[]{-18.673f, -15.810f, -13.307f, -14.852f, -14.617f, -11.037f, -10.001f},
                new float[]{15.040f, 16.074f, 14.408f, 16.171f, 19.584f, 18.902f, 17.996f },
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));

        List<Integer> indices = List.of(0, 1, 2, 3, 4, 5, 6);
        List<Map<String, float[]>> residues1 = indices.stream().map(structure1::manifestResidue).collect(Collectors.toList());
        List<Map<String, float[]>> residues2 = indices.stream().map(structure2::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignmentResult = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(0.719106, alignmentResult.getRootMeanSquareDeviation(), Helpers.DELTA); // 0.001 precision
    }

    @Test
    public void whenSelfAlign_thenRmsdIsZeroAndTransformationIsIdentity() {
        Structure structure = new Structure("1tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2),
                new int[] { 0, 1, 2 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA" },
                new float[]{6.994f, 9.429f, 5.547f},
                new float[]{8.354f, 7.479f, 0.158f},
                new float[]{42.405f, 48.266f, 42.050f},
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<String, float[]>> residues = indices.stream().map(structure::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment11 = alignmentService.align(residues, residues, AtomPairingScheme.ALL);

        double rmsd11 = alignment11.getRootMeanSquareDeviation();
        float[] transformation11 = alignment11.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { 1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1 }, transformation11, (float) Helpers.DELTA);
        assertEquals(0.0, rmsd11, Helpers.DELTA);
    }

    @Test
    public void whenAlign1And2_thenRmsdAndTransformationMatches() {
        Structure structure1 = new Structure("1tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2),
                new int[] { 0, 1, 2 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA" },
                new float[]{6.994f, 9.429f, 5.547f},
                new float[]{8.354f, 7.479f, 0.158f},
                new float[]{42.405f, 48.266f, 42.050f},
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));
        Structure structure2 = new Structure("2tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2),
                new int[] { 0, 1, 2 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA" },
                new float[]{3.908f, 4.588f, 12.080f},
                new float[]{12.066f, 6.531f, 12.645f},
                new float[]{-6.159f, -9.119f, -7.073f},
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<String, float[]>> residues1 = indices.stream().map(structure1::manifestResidue).collect(Collectors.toList());
        List<Map<String, float[]>> residues2 = indices.stream().map(structure2::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment12 = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);

        double rmsd12 = alignment12.getRootMeanSquareDeviation();
        float[] transformation12 = alignment12.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { -0.0400830537616845f, -0.7695568423369796f, 0.6373190843003801f, 20.360654211896712f,
                -0.997265515423408f, 0.07044448040859017f, 0.022339805891020748f, 11.603075263299026f,
                -0.062087362227933755f, -0.6346808974534377f, -0.7702761309162296f, 45.53692278711121f,
                0, 0, 0, 1 }, transformation12, (float) Helpers.DELTA); // 0.001
        assertEquals(0.19986, rmsd12, Helpers.DELTA); // 0.001
    }

    @Test
    public void whenAlign3And4_thenRmsdAndTransformationMatches() {
        Structure structure3 = new Structure("1tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2),
                new int[] { 0, 1, 2 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[] { "CA", "CA", "CA" },
                new float[]{5.055f, 7.320f, 6.021f},
                new float[]{74.865f, 76.960f, 74.874f},
                new float[]{22.585f, 20.325f, 17.385f},
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));
        Structure structure4 = new Structure("2tst",
                Map.of(new LabelSelection.SparseLabelSelection("A", 1), 0,
                new LabelSelection.SparseLabelSelection("A", 2), 1,
                new LabelSelection.SparseLabelSelection("A", 3), 2),
                new int[] { 0, 1, 2 },
                new ResidueType[] { ResidueType.HISTIDINE, ResidueType.ASPARTIC_ACID, ResidueType.SERINE, ResidueType.LYSINE, ResidueType.LYSINE, ResidueType.SERINE, ResidueType.PHENYLALANINE },
                new String[]{ "CA", "CA", "CA" },
                new float[]{5.055f, 7.321f, 6.020f},
                new float[]{74.864f, 76.962f, 74.873f},
                new float[]{22.583f, 20.326f, 17.386f},
                Map.of("1", List.of("A")),
                Map.of("1", Transformation.IDENTITY_TRANSFORMATION));

        List<Integer> indices = List.of(0, 1, 2);
        List<Map<String, float[]>> residues1 = indices.stream().map(structure3::manifestResidue).collect(Collectors.toList());
        List<Map<String, float[]>> residues2 = indices.stream().map(structure4::manifestResidue).collect(Collectors.toList());
        AlignmentResult alignment34 = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);

        double rmsd34 = alignment34.getRootMeanSquareDeviation();
        float[] transformation34 = alignment34.getTransformation().getFlattenedTransformation();
        assertArrayEquals(new float[] { 0.9999999693395323f, 2.476156855096834E-4f, 2.7214443547168843E-6f, -0.018765917915588126f,
                -2.4761583288007705E-4f, 0.9999999678697555f, 5.428524155062786E-5f, 4.2976537221761646E-4f,
                -2.70800238995339E-6f, -5.428591375893077E-5f, 0.9999999985228533f, 0.004118822613254025f,
                0, 0, 0, 1 }, transformation34, (float) Helpers.DELTA); // 0.001
        assertEquals(0.0021, rmsd34, Helpers.DELTA); // 0.001
    }

    @Test
    public void whenAminopeptidaseExampleWithAllAtoms_thenRmsdInflated() {
        Structure structure1 = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        List<Map<String, float[]>> residues1 = Stream.of("A-250", "A-255", "A-273", "A-332", "A-334")
                .map(id -> id.split("-"))
                .map(split -> structure1.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure1::manifestResidue)
                .collect(Collectors.toList());
        Structure structure2 = structureReader.readFromInputStream(getOriginalBcif("3pei"));
        List<Map<String, float[]>> residues2 = Stream.of("A-251", "A-256", "A-274", "A-333", "A-335")
                .map(id -> id.split("-"))
                .map(split -> structure2.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure2::manifestResidue)
                .collect(Collectors.toList());

        AlignmentResult result = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(3.167, result.getRootMeanSquareDeviation(), Helpers.DELTA);
    }

    @Test
    public void whenAminopeptidaseExampleWithNonAmbiguous_thenRmsdMatches() {
        Set<String> ambiguous = Set.of("OD1", "OD2", "OE1", "OE2");
        Structure structure1 = structureReader.readFromInputStream(getOriginalBcif("1lap"));
        List<Map<String, float[]>> residues1 = Stream.of("A-250", "A-255", "A-273", "A-332", "A-334")
                .map(id -> id.split("-"))
                .map(split -> structure1.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure1::manifestResidue)
                // manually filter away ambiguous atoms of GLU
                .map(map -> map.entrySet().stream().filter(entry -> !ambiguous.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
        Structure structure2 = structureReader.readFromInputStream(getOriginalBcif("3pei"));
        List<Map<String, float[]>> residues2 = Stream.of("A-251", "A-256", "A-274", "A-333", "A-335")
                .map(id -> id.split("-"))
                .map(split -> structure2.getResidueIndex(split[0], Integer.parseInt(split[1])))
                .map(structure2::manifestResidue)
                // manually filter away ambiguous atoms of GLU
                .map(map -> map.entrySet().stream().filter(entry -> !ambiguous.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());

        AlignmentResult result = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(2.211, result.getRootMeanSquareDeviation(), Helpers.DELTA);
    }
}
