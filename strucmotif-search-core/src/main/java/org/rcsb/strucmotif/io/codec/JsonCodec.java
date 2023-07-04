package org.rcsb.strucmotif.io.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Text-based implementation of a codec serializer. Useful for debugging.
 */
public class JsonCodec implements BucketCodec {
    public static final String STRUCTURE_INDICES = "structure_indices";
    public static final String POSITION_OFFSETS = "position_offsets";
    public static final String IDENTIFIER_DATA = "identifier_data";
    private final Type mapType;
    private final Gson gson;

    /**
     * Default constructor.
     */
    public JsonCodec() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.mapType = new TypeToken<Map<String, int[]>>() {}.getType();
    }

    @Override
    public InvertedIndexBucket decode(ByteBuffer byteBuffer) {
        Map<String, int[]> map = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(toByteArray(byteBuffer))), mapType);
        return new InvertedIndexBucket(map.get(STRUCTURE_INDICES), map.get(POSITION_OFFSETS), map.get(IDENTIFIER_DATA));
    }

    private byte[] toByteArray(ByteBuffer byteBuffer) {
        byteBuffer.rewind();
        byte[] out = new byte[byteBuffer.remaining()];
        byteBuffer.get(out);
        return out;
    }

    @Override
    public ByteBuffer encode(InvertedIndexBucket invertedIndexBucket) {
        Map<String, int[]> map = Map.of(STRUCTURE_INDICES, invertedIndexBucket.getStructureIndexArray(), POSITION_OFFSETS, invertedIndexBucket.getPositionOffsetArray(), IDENTIFIER_DATA, invertedIndexBucket.getIdentifierDataArray());
        byte[] bytes = gson.toJson(map).getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.wrap(bytes);
    }
}
