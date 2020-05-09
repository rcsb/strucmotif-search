package org.rcsb.strucmotif.domain;

public class RootMeanSquareDeviation implements AlignmentScore {
    private final double score;

    public RootMeanSquareDeviation(double score) {
        this.score = score;
    }

    @Override
    public double doubleValue() {
        return score;
    }
}
