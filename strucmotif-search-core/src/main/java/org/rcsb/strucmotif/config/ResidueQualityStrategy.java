package org.rcsb.strucmotif.config;

import java.util.function.BiPredicate;

/**
 * Specify which residues to index and which to ignore.
 */
public enum ResidueQualityStrategy implements BiPredicate<Double, Double> {
    /**
     * Index all residues.
     */
    NONE((v, c) -> true),
    /**
     * Index only residues with a B-factor above the specified threshold.
     */
    BFACTOR_ABOVE_CUTOFF((v, c) -> v > c),
    /**
     * Index only residues with a B-factor below the specified threshold.
     */
    BFACTOR_BELOW_CUTOFF((v, c) -> v < c),
    /**
     * Index only residues with a B-factor above the specified threshold.
     */
    QA_METRIC_LOCAL_ABOVE_CUTOFF((v, c) -> v > c),
    /**
     * Index only residues with a B-factor below the specified threshold.
     */
    QA_METRIC_LOCAL_BELOW_CUTOFF((v, c) -> v < c);

    private final BiPredicate<Double, Double> predicate;

    ResidueQualityStrategy(BiPredicate<Double, Double> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(Double value, Double cutoff) {
        return predicate.test(value, cutoff);
    }
}
