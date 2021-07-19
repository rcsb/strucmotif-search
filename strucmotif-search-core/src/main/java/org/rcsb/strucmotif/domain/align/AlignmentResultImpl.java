package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.score.RootMeanSquareDeviation;

/**
 * The result of an alignment.
 */
public class AlignmentResultImpl implements AlignmentResult {
    private final Transformation transformation;
    private final RootMeanSquareDeviation rootMeanSquareDeviation;

    /**
     * Construct an alignment result.
     * @param transformation applied transformation
     * @param rootMeanSquareDeviation achieved score
     */
    public AlignmentResultImpl(Transformation transformation, RootMeanSquareDeviation rootMeanSquareDeviation) {
        this.transformation = transformation;
        this.rootMeanSquareDeviation = rootMeanSquareDeviation;
    }

    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public RootMeanSquareDeviation getRootMeanSquareDeviation() {
        return rootMeanSquareDeviation;
    }
}
