package org.rcsb.strucmotif.domain.score;

public class GeometricDescriptorScore implements Score {
    private static final double BACKBONE_WEIGHT = 0.333;
    private static final double SIDE_CHAIN_WEIGHT = 0.333;
    private static final double ANGLE_WEIGHT = 0.333;

    private final double backboneScore;
    private final double sideChainScore;
    private final double angleScore;
    private final double score;

    public GeometricDescriptorScore(double backboneScore, double sideChainScore, double angleScore) {
        this.backboneScore = backboneScore;
        this.sideChainScore = sideChainScore;
        this.angleScore = angleScore;
        this.score = 1 - (BACKBONE_WEIGHT * backboneScore + SIDE_CHAIN_WEIGHT * sideChainScore + ANGLE_WEIGHT * angleScore);
    }

    @Override
    public double value() {
        return score;
    }

    public double getBackboneScore() {
        return backboneScore;
    }

    public double getSideChainScore() {
        return sideChainScore;
    }

    public double getAngleScore() {
        return angleScore;
    }
}
