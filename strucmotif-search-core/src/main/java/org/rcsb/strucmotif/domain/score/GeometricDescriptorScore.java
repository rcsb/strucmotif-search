package org.rcsb.strucmotif.domain.score;

public class GeometricDescriptorScore implements Score {
    private static final double BACKBONE_WEIGHT = 1;
    private static final double SIDE_CHAIN_WEIGHT = 1;
    private static final double ANGLE_WEIGHT = 1;

    private final int backboneScore;
    private final int sideChainScore;
    private final int angleScore;
    private final double score;

    public GeometricDescriptorScore(int backboneScore, int sideChainScore, int angleScore) {
        this.backboneScore = backboneScore;
        this.sideChainScore = sideChainScore;
        this.angleScore = angleScore;
        this.score = BACKBONE_WEIGHT * backboneScore + SIDE_CHAIN_WEIGHT * sideChainScore + ANGLE_WEIGHT * angleScore;
    }

    @Override
    public double value() {
        return score;
    }

    public int getBackboneScore() {
        return backboneScore;
    }

    public int getSideChainScore() {
        return sideChainScore;
    }

    public int getAngleScore() {
        return angleScore;
    }
}
