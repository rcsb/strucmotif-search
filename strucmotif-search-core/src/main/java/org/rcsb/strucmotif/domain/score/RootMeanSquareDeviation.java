package org.rcsb.strucmotif.domain.score;

/**
 * Score a hit by its root-mean-square deviation.
 */
public class RootMeanSquareDeviation implements Score {
    private final float score;

    /**
     * Construct a RMSD instance by wrapping a double value.
     * @param score the computed score
     */
    public RootMeanSquareDeviation(float score) {
        this.score = score;
    }

    @Override
    public float value() {
        return score;
    }
}
