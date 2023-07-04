package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Persistence of inverted index buckets.
 */
public interface BucketCodec {
    /**
     * Deserialize a bucket from an InputStream.
     * @param byteBuffer data source
     * @return the decoded bucket
     */
    InvertedIndexBucket decode(ByteBuffer byteBuffer);

    /**
     * Serialize a bucket as binary stream.
     * @param bucket data source
     * @return the encoded data
     * @throws IOException writing failed
     */
    ByteBuffer encode(InvertedIndexBucket bucket) throws IOException;
}
