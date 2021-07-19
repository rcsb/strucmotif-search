package org.rcsb.strucmotif2.config;

/**
 * How to simplify/prune motifs?
 */
public enum MotifPruningStrategy {
    /**
     * Minimal spanning tree, determined by Kruskal's algorithm.
     */
    KRUSKAL,
    /**
     * Merely extract {@link org.rcsb.strucmotif2.domain.motif.ResiduePairOccurrence}.
     */
    NONE
}
