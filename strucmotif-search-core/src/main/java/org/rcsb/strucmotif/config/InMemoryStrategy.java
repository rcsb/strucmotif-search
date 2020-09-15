package org.rcsb.strucmotif.config;

public enum InMemoryStrategy {
    /**
     * Disable, BinaryCIF will be read from disk.
     */
    OFF,
    /**
     * Keep gzipped BinaryCIF in memory (uses less memory but spends some CPU cycles on decompressing data).
     */
    GZIPPED,
    /**
     * Keep uncompressed BinaryCIF in memory (uses more memory but saves CPU cycles as no decompression is needed).
     */
    PLAIN
}
