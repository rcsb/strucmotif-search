package org.rcsb.strucmotif.config;

import org.rcsb.strucmotif.io.codec.BucketCodec;
import org.rcsb.strucmotif.io.codec.ColferCodec;
import org.rcsb.strucmotif.io.codec.JsonCodec;

/**
 * How to persist inverted index data?
 */
public enum InvertedIndexBackend {
    /**
     * Smaller files that are faster to read/write in a less standard format.
     */
    COLFER(new ColferCodec(), ".colf"),
    /**
     * Useful for debugging.
     */
    JSON(new JsonCodec(), ".json");

    private final BucketCodec bucketCodec;
    private final String extension;

    InvertedIndexBackend(BucketCodec bucketCodec, String extension) {
        this.bucketCodec = bucketCodec;
        this.extension = extension;
    }

    /**
     * The implementation used by this backend.
     * @return a {@link BucketCodec} instance
     */
    public BucketCodec getBucketCodec() {
        return bucketCodec;
    }

    /**
     * Reports the extension the index files have.
     * @return the extension associated with this backend
     */
    public String getExtension() {
        return extension;
    }
}
