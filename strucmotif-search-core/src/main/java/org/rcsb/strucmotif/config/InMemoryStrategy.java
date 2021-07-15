package org.rcsb.strucmotif.config;

/**
 * Keep index and structure data in memory? Requires ~100 GB of memory.
 */
public enum InMemoryStrategy {
    /**
     * Access file-system any time data is requested.
     */
    DISABLED,
    /**
     * Load all data into heap during initialization. // TODO there are ~300M residues in the archive - investigate an in-memory solution to access coordinates
     */
    HEAP
}
