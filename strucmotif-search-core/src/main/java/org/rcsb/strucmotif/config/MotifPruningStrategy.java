package org.rcsb.strucmotif.config;

/**
 * How to simplify/prune motifs?
 */
public enum MotifPruningStrategy {
    /**
     * Minimal spanning tree, determined by Kruskal's algorithm.
     */
    KRUSKAL,
    /**
     * Merely extract {@link org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence}.
     */
    NONE
}
