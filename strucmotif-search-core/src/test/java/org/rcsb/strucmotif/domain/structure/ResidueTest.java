package org.rcsb.strucmotif.domain.structure;

import org.junit.Test;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class ResidueTest {
    @Test
    public void shouldCreateVirtualBetaCarbon() {
        Residue glycine = StructureFactory.createResidue(new ResidueIdentifier("GLY", 1, 1),
                List.of(StructureFactory.createAtom(new AtomIdentifier("N", 1), new double[] { -0.966, 0.493, 1.500 }),
                        StructureFactory.createAtom(new AtomIdentifier("CA", 2), new double[] { 0.257, 0.418, 0.692 }),
                        StructureFactory.createAtom(new AtomIdentifier("C", 3), new double[] { -0.094, 0.017, -0.716 }),
                        StructureFactory.createAtom(new AtomIdentifier("O",4 ), new double[] { -1.056, -0.682, -0.923 })),
                Matrix4DTransformation.IDENTITY_MATRIX_4D);

        double[] betaCarbon = glycine.getSideChainCoordinates();
        System.out.println(Arrays.toString(betaCarbon));

        assertArrayEquals(new double[] { 1.204, -0.620, 1.296 }, betaCarbon, 0.01);
    }
}
