package org.rcsb.strucmotif.align;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rcsb.strucmotif.GuiceJUnit4Runner;
import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.io.read.AllPurposeReaderImpl;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.rcsb.strucmotif.domain.Matrix4DTransformation.IDENTITY_MATRIX_4D;

@RunWith(GuiceJUnit4Runner.class)
public class QuaternionAlignmentTest {
    @Inject
    private Alignment alignment;
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
    public void originalExample() {
        // https://theobald.brandeis.edu/qcp/main.c
        List<Residue> container1 = List.of(
                create("HIS", new double[] { -2.803, -15.373, 24.556 }),
                create("ASP", new double[] { 0.893, -16.062, 25.147 }),
                create("SER", new double[] { 1.368, -12.371, 25.885 }),
                create("LYS", new double[] { -1.651, -12.153,28.177 }),
                create("LYS", new double[] { -0.440, -15.218, 30.068 }),
                create("SER", new double[] { 2.551,  -13.273, 31.372 }),
                create("PHE", new double[] { 0.105, -11.330, 33.567 }));

        List<Residue> container2 = List.of(
                create("HIS", new double[] { -14.739, -18.673, 15.040 }),
                create("ASP", new double[] { -12.473, -15.810, 16.074 }),
                create("SER", new double[] { -14.802, -13.307, 14.408 }),
                create("LYS", new double[] { -17.782, -14.852, 16.171 }),
                create("LYS", new double[] { -16.124, -14.617, 19.584 }),
                create("SER", new double[] { -15.029, -11.037, 18.902 }),
                create("PHE", new double[] { -18.577, -10.001, 17.996 }));

        AlignmentResult alignmentResult = alignment.align(container1, container2, AtomPairingScheme.ALL);

        assertEquals(0.719106, alignmentResult.getScore().doubleValue(), 0.001);
    }

    @Test
    public void alignPoints() {
        List<Residue> container1 = List.of(
                create("HIS", new double[] { 6.994, 8.354, 42.405 }),
                create("ASP", new double[] { 9.429, 7.479, 48.266 }),
                create("SER", new double[] { 5.547, 0.158, 42.050 }));
        List<Residue> container2 = List.of(
                create("HIS", new double[] { 3.908, 12.066, -6.159 }),
                create("ASP", new double[] { 4.588, 6.531, -9.119 }),
                create("SER", new double[] { 12.080, 12.645, -7.073 }));
        List<Residue> container3 = List.of(
                create("HIS", new double[] { 5.055, 74.865, 22.585 }),
                create("ASP", new double[] { 7.320, 76.960, 20.325 }),
                create("SER", new double[] { 6.021, 74.874, 17.385 }));
        List<Residue> container4 = List.of(
                create("HIS", new double[] { 5.055, 74.864, 22.583 }),
                create("ASP", new double[] { 7.321, 76.962, 20.326 }),
                create("SER", new double[] { 6.020, 74.873, 17.386 }));

        AlignmentResult alignment11 = alignment.align(container1, container1, AtomPairingScheme.ALL);
        AlignmentResult alignment12 = alignment.align(container1, container2, AtomPairingScheme.ALL);
        AlignmentResult alignment34 = alignment.align(container3, container4, AtomPairingScheme.ALL);

        double rmsd11 = alignment11.getScore().doubleValue();
        double[] transformation11 = flatten(alignment11.getTransformation().getTransformation());
        assertArrayEquals(new double[] { 1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1 }, transformation11, 0.001);
        assertEquals("Self alignment should be perfect", 0.0, rmsd11, 0.001);

        double rmsd12 = alignment12.getScore().doubleValue();
        double[] transformation12 = flatten(alignment12.getTransformation().getTransformation());
        assertArrayEquals(new double[] { -0.0400830537616845, -0.7695568423369796, 0.6373190843003801, 20.360654211896712,
                -0.997265515423408, 0.07044448040859017, 0.022339805891020748, 11.603075263299026,
                -0.062087362227933755, -0.6346808974534377, -0.7702761309162296, 45.53692278711121,
                0, 0, 0, 1 }, transformation12, 0.001);
        assertEquals("Alignment score does not match expectation", 0.19986, rmsd12, 0.001);

        double rmsd34 = alignment34.getScore().doubleValue();
        double[] transformation34 = flatten(alignment34.getTransformation().getTransformation());
        assertArrayEquals(new double[] { 0.9999999693395323, 2.476156855096834E-4, 2.7214443547168843E-6, -0.018765917915588126,
                -2.4761583288007705E-4, 0.9999999678697555, 5.428524155062786E-5, 4.2976537221761646E-4,
                -2.70800238995339E-6, -5.428591375893077E-5, 0.9999999985228533, 0.004118822613254025,
                0, 0, 0, 1 }, transformation34, 0.001);
        assertEquals("Alignment score does not match expectation", 0.0021, rmsd34, 0.001);
    }

    private static InputStream getInputStream(String pdbId) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("orig/" + pdbId + ".bcif");
    }

    @Test
    public void computeRmsdForAminopeptidaseExample() {
        AllPurposeReaderImpl allPurposeReader = new AllPurposeReaderImpl();
        Structure s1 = allPurposeReader.readFromInputStream(getInputStream("1lap"), List.of(new LabelSelection("A", 1, 250),
                new LabelSelection("A", 1, 255),
                new LabelSelection("A", 1, 273),
                new LabelSelection("A", 1, 332),
                new LabelSelection("A", 1, 334)));
        Structure s2 = allPurposeReader.readFromInputStream(getInputStream("3pei"), List.of(new LabelSelection("A", 1, 251),
                new LabelSelection("A", 1, 256),
                new LabelSelection("A", 1, 274),
                new LabelSelection("A", 1, 333),
                new LabelSelection("A", 1, 335)));
        System.out.println(alignment.align(s1.getChains().stream().map(Chain::getResidues).flatMap(Collection::stream).collect(Collectors.toList()),
                s2.getChains().stream().map(Chain::getResidues).flatMap(Collection::stream).collect(Collectors.toList()),
                AtomPairingScheme.ALL).getScore().doubleValue());
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
