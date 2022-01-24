package org.rcsb.strucmotif.config;

/**
 * How to persist inverted index data?
 */
public enum InvertedIndexBackend {
    /**
     * Smaller files that are faster to read/write in a less standard format.
     */
    COLFER,
    /**
     * Less compact and slower MessagePack in a common serialization format.
     */
    MESSAGE_PACK
}
