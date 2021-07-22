package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.domain.Transformation;

/**
 * The result of an alignment.
 */
public class AlignmentResultImpl implements AlignmentResult {
    private final Transformation transformation;
    private final float rmsd;

    /**
     * Construct an alignment result.
     * @param transformation applied transformation
     * @param rmsd achieved score
     */
    public AlignmentResultImpl(Transformation transformation, float rmsd) {
        this.transformation = transformation;
        this.rmsd = rmsd;
    }

    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public float getRootMeanSquareDeviation() {
        return rmsd;
    }
}
