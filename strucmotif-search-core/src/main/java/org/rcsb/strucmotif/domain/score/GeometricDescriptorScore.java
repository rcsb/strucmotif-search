package org.rcsb.strucmotif.domain.score;

/**
 * A composite score derived from geometric properties of this hit.
 */
public class GeometricDescriptorScore implements Score {
    private static final double BACKBONE_WEIGHT = 0.333;
    private static final double SIDE_CHAIN_WEIGHT = 0.333;
    private static final double ANGLE_WEIGHT = 0.333;

    private final double backboneScore;
    private final double sideChainScore;
    private final double angleScore;
    private final double score;

    /**
     * Construct a geometric descriptor score, based on geometric properties.
     * @param backboneScore the backbone score
     * @param sideChainScore the side-chain score
     * @param angleScore the angle score
     */
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

    /**
     * Original value of the backbone component.
     * @return a double
     */
    public double getBackboneScore() {
        return backboneScore;
    }

    /**
     * Original value of the side-chain component.
     * @return a double
     */
    public double getSideChainScore() {
        return sideChainScore;
    }

    /**
     * Original value of the angle component.
     * @return a double
     */
    public double getAngleScore() {
        return angleScore;
    }
}
