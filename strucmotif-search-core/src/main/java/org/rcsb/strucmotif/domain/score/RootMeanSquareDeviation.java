package org.rcsb.strucmotif.domain.score;

public class RootMeanSquareDeviation implements Score {
    private final double score;

    public RootMeanSquareDeviation(double score) {
        this.score = score;
    }

    @Override
    public double value() {
        return score;
    }
}
