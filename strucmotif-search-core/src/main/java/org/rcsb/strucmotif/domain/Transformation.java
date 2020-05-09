package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

public interface Transformation {
    boolean isNeutral();

    double[][] getTransformation();

    List<Residue> transformComponents(List<Residue> residues);

    Residue transformComponent(Residue residue);

    List<Atom> transformAtoms(List<Atom> atoms);

    Atom transformAtom(Atom atom);

    List<double[]> transformVectors(List<double[]> vectors);

    double[] transformVector(double[] vector);
}
