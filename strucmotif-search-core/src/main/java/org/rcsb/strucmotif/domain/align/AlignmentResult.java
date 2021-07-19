package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.score.RootMeanSquareDeviation;

import java.util.List;

/**
 * The result of an alignment.
 */
public interface AlignmentResult {
    /**
     * The transformation which recreates this alignment when applied to the 2nd argument.
     * @return a {@link org.rcsb.strucmotif.domain.Transformation} which can be used to recreate this alignment
     */
    Transformation getTransformation();

    /**
     * The score of this superposition.
     * @return an {@link RootMeanSquareDeviation}
     */
    RootMeanSquareDeviation getRootMeanSquareDeviation();
}
