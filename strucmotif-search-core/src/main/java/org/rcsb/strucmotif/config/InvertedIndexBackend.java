package org.rcsb.strucmotif.config;

/**
 * How to persist inverted index data?
 */
public enum InvertedIndexBackend {
    /**
     * Smaller files that are faster to read/write.
     */
    COLFER,
    /**
     * Less compact and slower MessagePack.
     */
    MESSAGE_PACK;
}
