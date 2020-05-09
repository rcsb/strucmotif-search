package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Matrix4DTransformation implements Transformation {
    public static final double[][] IDENTITY_MATRIX_4D = new double[][] {
            { 1, 0, 0, 0 },
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 0, 0, 0, 1 }
    };
    private final double[][] rotation;
    private final double[] translation;
    private final double[][] transformation;

    public Matrix4DTransformation(double[] translation, double[][] rotation) {
        this(translation, rotation, Algebra.composeTransformationMatrix(rotation, translation));
    }

    public Matrix4DTransformation(double[] translation, double[][] rotation, double[][] transformation) {
        this.translation = translation;
        this.rotation = rotation;
        this.transformation = transformation;
    }

    public double[][] getRotation() {
        return rotation;
    }

    public double[] getTranslation() {
        return translation;
    }

    @Override
    public double[][] getTransformation() {
        return transformation;
    }

    @Override
    public List<Residue> transformComponents(List<Residue> residues) {
        List<Residue> values = new ArrayList<>(residues.size());
        for (Residue residue : residues) {
            values.add(transformComponent(residue));
        }
        return values;
    }

    @Override
    public Residue transformComponent(Residue residue) {
        return StructureFactory.createResidue(residue.getResidueIdentifier(), residue.getAtoms(), transformation);
    }

    @Override
    public List<Atom> transformAtoms(List<Atom> atoms) {
        List<Atom> values = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            values.add(transformAtom(atom));
        }
        return values;
    }

    @Override
    public Atom transformAtom(Atom atom) {
        return StructureFactory.createAtom(atom.getAtomIdentifier(), Algebra.multiply4d(transformation, atom.getCoord()));
    }

    @Override
    public List<double[]> transformVectors(List<double[]> vectors) {
        List<double[]> values = new ArrayList<>(vectors.size());
        for (double[] vector : vectors) {
            values.add(transformVector(vector));
        }
        return values;
    }

    @Override
    public double[] transformVector(double[] vector) {
        return Algebra.multiply4d(transformation, vector);
    }

    @Override
    public boolean isNeutral() {
        return Arrays.deepEquals(IDENTITY_MATRIX_4D, transformation);
    }
}
