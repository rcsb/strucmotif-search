package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.bucket.Bucket;
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
    ByteBuffer encode(Bucket bucket) throws IOException;

    /**
     * Serialize raw arrays as binary stream.
     * @param structureIndices index array
     * @param positionOffsets offset array
     * @param positionData data array
     * @param operatorIndices sparse operator indices
     * @param operatorData sparse operator data
     * @return the encoded data
     * @throws IOException writing failed
     */
    ByteBuffer encode(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) throws IOException;
}
