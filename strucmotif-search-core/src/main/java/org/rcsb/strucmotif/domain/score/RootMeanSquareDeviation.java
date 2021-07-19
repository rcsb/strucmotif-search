package org.rcsb.strucmotif.domain.score;

/**
 * Score a hit by its root-mean-square deviation.
 */
public class RootMeanSquareDeviation implements Score {
    private final double score;

    /**
     * Construct a RMSD instance by wrapping a double value.
     * @param score the computed score
     */
    public RootMeanSquareDeviation(double score) {
        this.score = score;
    }

    @Override
    public double value() {
        return score;
    }
}
