package org.rcsb.strucmotif.config;

/**
 * Keep structure data in memory? Requires ~30 GB of memory for 180k structures.
 */
public enum InMemoryStrategy {
    /**
     * Access file-system any time structure data is requested.
     */
    OFF,
    /**
     * Load all data into heap during initialization.
     */
    HEAP
}
