package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

/**
 * A transformation described by a 4x4 matrix.
 */
public interface Transformation {
    /**
     * <code>true</code> if this is the identity matrix
     * @return a boolean
     */
    boolean isNeutral();

    /**
     * The actual transformation matrix.
     * @return a 4x4 transformation matrix
     */
    double[][] getTransformation();

    /**
     * Transform a collection of residues.
     * @param residues the residues to transform
     * @return a transformed collection
     */
    List<Residue> transformComponents(List<Residue> residues);

    /**
     * Transform a residue.
     * @param residue the residue to transform
     * @return a transformed instance
     */
    Residue transformComponent(Residue residue);

    /**
     * Transform a collection of atoms.
     * @param atoms the atoms to transform
     * @return a transformed collection
     */
    List<Atom> transformAtoms(List<Atom> atoms);

    /**
     * Transform an atom.
     * @param atom the atom to transform
     * @return a transformed instance
     */
    Atom transformAtom(Atom atom);

    /**
     * Transform a collection of vectors.
     * @param vectors the vectors to transform
     * @return a transformed collection
     */
    List<double[]> transformVectors(List<double[]> vectors);

    /**
     * Transform a vector.
     * @param vector the vector to transform
     * @return a transformed instance
     */
    double[] transformVector(double[] vector);
}
