package org.rcsb.strucmotif.config;

/**
 * Keep index and structure data in memory? Requires ~100 GB of memory.
 */
public enum InMemoryStrategy {
    /**
     * Access file-system any time data is requested.
     */
    OFF,
    /**
     * Load all data into heap during initialization.
     */
    HEAP
}
