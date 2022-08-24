package org.rcsb.strucmotif.io.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Text-based implementation of a codec serializer. Useful for debugging.
 */
public class JsonCodec extends AbstractBucketCodec {
    private final Gson gson;

    /**
     * Default constructor.
     */
    public JsonCodec() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public InvertedIndexBucket decode(ByteBuffer byteBuffer) {
        ArrayBucket arrayBucket = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(toByteArray(byteBuffer))), ArrayBucket.class);
        return new InvertedIndexBucket(arrayBucket.getStructureIndices(),
                arrayBucket.getPositionOffsets(),
                arrayBucket.getPositionData(),
                arrayBucket.getOperatorIndices(),
                arrayBucket.getOperatorData());
    }

    private byte[] toByteArray(ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        byte[] out = new byte[byteBuffer.remaining()];
        byteBuffer.get(out);
        return out;
    }

    @Override
    public ByteBuffer encode(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
        ArrayBucket arrayBucket = new ArrayBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
        byte[] bytes = gson.toJson(arrayBucket).getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.wrap(bytes);
    }
}
