package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.align.QuaternionAlignment;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.math.Algebra;

import java.util.List;

/**
 * An amino acid in a structure.
 */
public class AminoAcid extends Residue {
    // already centered coordinates to save operations
    private static final List<double[]> REFERENCE_BACKBONE = List.of(new double[] { -0.698, 0.184, 1.008 }, // N
            new double[] { 0.525, 0.109, 0.200 }, // CA
            new double[] { 0.174, -0.292, -1.208 }); // C
    private static final double[] REFERENCE_CB = new double[] { 1.472, -0.929, 0.804 };
    private static final double[] REFERENCE_CENTROID = new double[3];

    private final Atom ca;
    private final Atom cb;

    /**
     * Construct a new amino acid.
     * @param residueIdentifier its identifier
     * @param atoms associated atoms
     * @param transformation transformation to apply
     */
    AminoAcid(ResidueIdentifier residueIdentifier, List<Atom> atoms, double[][] transformation) {
        super(residueIdentifier, atoms, transformation);
        this.ca = findAtomUnsafe("CA");
//      // will be null if glycine and no virtual CB created before (i.e. currently being center or used to create assembly)
        this.cb = findAtomUnsafe("CB");
    }

    /**
     * Access to the alpha carbon.
     * @return an atom
     */
    public Atom getCA() {
        return ca;
    }

    /**
     * Access to the beta carbon.
     * @return an atom
     */
    public Atom getCB() {
        if (cb != null) {
            return cb;
        } else {
            // CB will not be present for glycine - so we can't rely on the constructor to create virtual CB
            if (getResidueIdentifier().getResidueType() == ResidueType.GLYCINE) {
                return calculateVirtualBetaCarbon();
            } else {
                return null;
            }
        }
    }

    private Atom calculateVirtualBetaCarbon() {
        List<double[]> coordList = List.of(findAtomUnsafe("N").getCoord(),
                ca.getCoord(),
                findAtomUnsafe("C").getCoord());
        double[] centroid = Algebra.centroid3d(coordList);

        Matrix4DTransformation transformation = QuaternionAlignment.align(coordList, centroid, REFERENCE_BACKBONE, REFERENCE_CENTROID).getFirst();
        return StructureFactory.createAtom(new AtomIdentifier("CB", -1), transformation.transformVector(REFERENCE_CB));
    }

    @Override
    public double[] getSideChainCoordinates() {
        return coord(getCB());
    }

    @Override
    public double[] getBackboneCoordinates() {
        return coord(getCA());
    }
}
