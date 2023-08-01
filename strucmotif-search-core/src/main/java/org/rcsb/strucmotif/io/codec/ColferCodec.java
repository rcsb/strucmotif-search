package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.InputMismatchException;

/**
 * Serializes and deserializes buckets with a custom colfer implementation. See <a href="https://github.com/pascaldekloe/colfer">colfer</a>.
 */
public class ColferCodec implements BucketCodec {
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    @Override
    @SuppressWarnings("Duplicates")
    public InvertedIndexBucket decode(ByteBuffer byteBuffer) {
        int[] structureIndices = EMPTY_INT_ARRAY;
        int[] positionOffsets = EMPTY_INT_ARRAY;
        int[] identifierData = EMPTY_INT_ARRAY;

        byte header = byteBuffer.get();

        if (header == 0) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = byteBuffer.get();
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            structureIndices = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = byteBuffer.get();
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                structureIndices[ai] = x;
            }
            header = byteBuffer.get();
        }

        if (header == 1) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = byteBuffer.get();
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            positionOffsets = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = byteBuffer.get();
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                positionOffsets[ai] = x;
            }
            header = byteBuffer.get();
        }

        if (header == 2) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = byteBuffer.get();
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            identifierData = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = byteBuffer.get();
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                identifierData[ai] = x;
            }
            header = byteBuffer.get();
        }

        if (header != 0x7f) {
            throw new InputMismatchException("colfer: unknown header at byte " + (byteBuffer.position() - 1));
        }

        return new InvertedIndexBucket(structureIndices, positionOffsets, identifierData);
    }

    private void encodeInternal(ByteArrayOutputStream out, int[] structureIndices, int[] positionOffset, int[] identifierData) throws IOException {
        if (structureIndices.length != 0) {
            out.write(0);
            writeIntArray(out, structureIndices);
        }
        if (positionOffset.length != 0) {
            out.write(1);
            writeIntArray(out, positionOffset);
        }
        if (identifierData.length != 0) {
            out.write(2);
            writeIntArray(out, identifierData);
        }

        out.write(0x7f);
    }

    private void writeIntArray(OutputStream out, int[] data) throws IOException {
        int x = data.length;
        while (x > 0x7f) {
            out.write(x | 0x80);
            x >>>= 7;
        }
        out.write(x);

        for (int v : data) {
            while (v > 0x7f) {
                out.write(v | 0x80);
                v >>>= 7;
            }
            out.write(v);
        }
    }

    @Override
    public ByteBuffer encode(InvertedIndexBucket bucket) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            encodeInternal(outputStream, bucket.getStructureIndexArray(), bucket.getPositionOffsetArray(), bucket.getIdentifierDataArray());
            return ByteBuffer.wrap(outputStream.toByteArray());
        }
    }
}
