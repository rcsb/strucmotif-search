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
     * Index only residues with a quality above the specified threshold.
     */
    ABOVE_CUTOFF((v, c) -> v > c),
    /**
     * Index only residues with a quality below the specified threshold.
     */
    BELOW_CUTOFF((v, c) -> v < c);

    private final BiPredicate<Double, Double> predicate;

    ResidueQualityStrategy(BiPredicate<Double, Double> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(Double value, Double cutoff) {
        return predicate.test(value, cutoff);
    }
}
