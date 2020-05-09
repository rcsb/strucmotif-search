package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

public interface AlignmentResult {
    List<Residue> getOriginalReference();

    List<Residue> getOriginalCandidate();

    List<Residue> getAlignedCandidate();

    Transformation getTransformation();

    RootMeanSquareDeviation getScore();
}
