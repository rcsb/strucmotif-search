package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Persistence of inverted index buckets.
 */
public interface BucketCodec {
    /**
     * Deserialize a bucket from an InputStream.
     * @param inputStream data source
     * @return the decoded bucket
     * @throws IOException reading failed
     */
    InvertedIndexBucket decode(InputStream inputStream) throws IOException;

    /**
     * Serialize a bucket as binary stream.
     * @param bucket data source
     * @return the encoded data stream
     * @throws IOException writing failed
     */
    ByteArrayOutputStream encode(Bucket bucket) throws IOException;
}
