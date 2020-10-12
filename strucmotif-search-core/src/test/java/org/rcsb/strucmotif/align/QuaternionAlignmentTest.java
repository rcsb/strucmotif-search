package org.rcsb.strucmotif.align;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.read.StructureReaderImpl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.domain.Transformation.IDENTITY_MATRIX_4D;

public class QuaternionAlignmentTest {
    private AlignmentService alignmentService;
    private StructureReader structureReader;

    @BeforeEach
    public void init() {
        StructureDataProvider structureDataProvider = Mockito.mock(StructureDataProvider.class);
        alignmentService = new QuaternionAlignmentService(structureDataProvider);
        structureReader = new StructureReaderImpl();
    }

    private static int seqId = 1;
    private static int atomId = 1;
    private static Residue create(String compId, double[] coords) {
        Residue residue = StructureFactory.createResidue(new ResidueIdentifier(compId, seqId, seqId),
                List.of(StructureFactory.createAtom(new AtomIdentifier("C", atomId), coords)),
                IDENTITY_MATRIX_4D);
        seqId++;
        atomId++;
        return residue;
    }

    @Test
    public void whenOriginalExample_thenRmsdMatches() {
        // https://theobald.brandeis.edu/qcp/main.c
        List<Residue> container1 = List.of(create("HIS", new double[] { -2.803, -15.373, 24.556 }),
                create("ASP", new double[] { 0.893, -16.062, 25.147 }),
                create("SER", new double[] { 1.368, -12.371, 25.885 }),
                create("LYS", new double[] { -1.651, -12.153,28.177 }),
                create("LYS", new double[] { -0.440, -15.218, 30.068 }),
                create("SER", new double[] { 2.551,  -13.273, 31.372 }),
                create("PHE", new double[] { 0.105, -11.330, 33.567 }));

        List<Residue> container2 = List.of(create("HIS", new double[] { -14.739, -18.673, 15.040 }),
                create("ASP", new double[] { -12.473, -15.810, 16.074 }),
                create("SER", new double[] { -14.802, -13.307, 14.408 }),
                create("LYS", new double[] { -17.782, -14.852, 16.171 }),
                create("LYS", new double[] { -16.124, -14.617, 19.584 }),
                create("SER", new double[] { -15.029, -11.037, 18.902 }),
                create("PHE", new double[] { -18.577, -10.001, 17.996 }));

        AlignmentResult alignmentResult = alignmentService.align(container1, container2, AtomPairingScheme.ALL);

        assertEquals(0.719106, alignmentResult.getRootMeanSquareDeviation().value(), Helpers.DELTA);
    }

    @Test
    public void whenSelfAlign_thenRmsdIsZeroAndTransformationIsIdentity() {
        List<Residue> container1 = List.of(create("HIS", new double[] { 6.994, 8.354, 42.405 }),
                create("ASP", new double[] { 9.429, 7.479, 48.266 }),
                create("SER", new double[] { 5.547, 0.158, 42.050 }));

        AlignmentResult alignment11 = alignmentService.align(container1, container1, AtomPairingScheme.ALL);

        double rmsd11 = alignment11.getRootMeanSquareDeviation().value();
        double[] transformation11 = flatten(alignment11.getTransformation().getTransformationMatrix());
        assertArrayEquals(new double[] { 1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1 }, transformation11, Helpers.DELTA);
        assertEquals(0.0, rmsd11, Helpers.DELTA);
    }

    @Test
    public void whenAlign1And2_thenRmsdAndTransformationMatches() {
        List<Residue> container1 = List.of(create("HIS", new double[] { 6.994, 8.354, 42.405 }),
                create("ASP", new double[] { 9.429, 7.479, 48.266 }),
                create("SER", new double[] { 5.547, 0.158, 42.050 }));
        List<Residue> container2 = List.of(create("HIS", new double[] { 3.908, 12.066, -6.159 }),
                create("ASP", new double[] { 4.588, 6.531, -9.119 }),
                create("SER", new double[] { 12.080, 12.645, -7.073 }));

        AlignmentResult alignment12 = alignmentService.align(container1, container2, AtomPairingScheme.ALL);

        double rmsd12 = alignment12.getRootMeanSquareDeviation().value();
        double[] transformation12 = flatten(alignment12.getTransformation().getTransformationMatrix());
        assertArrayEquals(new double[] { -0.0400830537616845, -0.7695568423369796, 0.6373190843003801, 20.360654211896712,
                -0.997265515423408, 0.07044448040859017, 0.022339805891020748, 11.603075263299026,
                -0.062087362227933755, -0.6346808974534377, -0.7702761309162296, 45.53692278711121,
                0, 0, 0, 1 }, transformation12, Helpers.DELTA);
        assertEquals(0.19986, rmsd12, Helpers.DELTA);
    }

    @Test
    public void whenAlign3And4_thenRmsdAndTransformationMatches() {
        List<Residue> container3 = List.of(create("HIS", new double[] { 5.055, 74.865, 22.585 }),
                create("ASP", new double[] { 7.320, 76.960, 20.325 }),
                create("SER", new double[] { 6.021, 74.874, 17.385 }));
        List<Residue> container4 = List.of(create("HIS", new double[] { 5.055, 74.864, 22.583 }),
                create("ASP", new double[] { 7.321, 76.962, 20.326 }),
                create("SER", new double[] { 6.020, 74.873, 17.386 }));

        AlignmentResult alignment34 = alignmentService.align(container3, container4, AtomPairingScheme.ALL);

        double rmsd34 = alignment34.getRootMeanSquareDeviation().value();
        double[] transformation34 = flatten(alignment34.getTransformation().getTransformationMatrix());
        assertArrayEquals(new double[] { 0.9999999693395323, 2.476156855096834E-4, 2.7214443547168843E-6, -0.018765917915588126,
                -2.4761583288007705E-4, 0.9999999678697555, 5.428524155062786E-5, 4.2976537221761646E-4,
                -2.70800238995339E-6, -5.428591375893077E-5, 0.9999999985228533, 0.004118822613254025,
                0, 0, 0, 1 }, transformation34, Helpers.DELTA);
        assertEquals(0.0021, rmsd34, Helpers.DELTA);
    }

    @Test
    public void whenAminopeptidaseExample_thenRmsdMatches() {
        Structure structure1 = structureReader.readFromInputStream(getOriginalBcif("1lap"),
                List.of(new LabelSelection("A", "1", 250),
                        new LabelSelection("A", "1", 255),
                        new LabelSelection("A", "1", 273),
                        new LabelSelection("A", "1", 332),
                        new LabelSelection("A", "1", 334)));
        Structure structure2 = structureReader.readFromInputStream(getOriginalBcif("3pei"),
                List.of(new LabelSelection("A", "1", 251),
                        new LabelSelection("A", "1", 256),
                        new LabelSelection("A", "1", 274),
                        new LabelSelection("A", "1", 333),
                        new LabelSelection("A", "1", 335)));

        List<Residue> residues1 = structure1.getChains().stream().map(Chain::getResidues).flatMap(Collection::stream).collect(Collectors.toList());
        List<Residue> residues2 = structure2.getChains().stream().map(Chain::getResidues).flatMap(Collection::stream).collect(Collectors.toList());

        AlignmentResult result = alignmentService.align(residues1, residues2, AtomPairingScheme.ALL);
        assertEquals(2.211, result.getRootMeanSquareDeviation().value(), Helpers.DELTA);
    }

    private double[] flatten(double[][] transformation) {
        double[] re = new double[16];

        int element = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                re[element++] = transformation[i][j];
            }
        }

        return re;
    }
}
