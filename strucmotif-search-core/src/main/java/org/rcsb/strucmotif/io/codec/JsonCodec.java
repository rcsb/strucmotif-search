package org.rcsb.strucmotif.io.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public InvertedIndexBucket decode(InputStream inputStream) throws IOException {
        ArrayBucket arrayBucket = gson.fromJson(new InputStreamReader(inputStream), ArrayBucket.class);
        return new InvertedIndexBucket(arrayBucket.getStructureIndices(),
                arrayBucket.getPositionOffsets(),
                arrayBucket.getPositionData(),
                arrayBucket.getOperatorIndices(),
                arrayBucket.getOperatorData());
    }

    @Override
    public ByteArrayOutputStream encode(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) throws IOException {
        ArrayBucket arrayBucket = new ArrayBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
        byte[] bytes = gson.toJson(arrayBucket).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(bytes);
        return outputStream;
    }
}
