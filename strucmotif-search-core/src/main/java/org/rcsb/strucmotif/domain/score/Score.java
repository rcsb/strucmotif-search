package org.rcsb.strucmotif.domain.score;

/**
 * Scores a hit by a single value.
 */
public interface Score {
    /**
     * Get the actual score.
     * @return a double value
     */
    double value();
}
