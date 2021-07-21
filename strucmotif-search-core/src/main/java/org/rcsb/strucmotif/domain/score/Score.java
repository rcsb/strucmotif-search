package org.rcsb.strucmotif.domain.score;

/**
 * Scores a hit by a single value.
 */
public interface Score {
    /**
     * Get the actual score.
     * @return a float value
     */
    float value();
}
