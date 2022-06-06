package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;

/**
 * Serializes and deserializes buckets with a custom colfer implementation. See https://github.com/pascaldekloe/colfer.
 */
public class ColferCodec extends AbstractBucketCodec {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @Override
    public InvertedIndexBucket decode(InputStream inputStream) throws IOException {
        byte[] buf = inputStream.readAllBytes();
        return decodeInternal(buf);
    }

    @SuppressWarnings("Duplicates")
    private InvertedIndexBucket decodeInternal(byte[] buf) {
        int[] structureIndices = EMPTY_INT_ARRAY;
        int[] positionOffsets = EMPTY_INT_ARRAY;
        int[] positionData = EMPTY_INT_ARRAY;
        int[] operatorIndices = EMPTY_INT_ARRAY;
        String[] operatorData = EMPTY_STRING_ARRAY;

        int i = 0;
        byte header = buf[i++];

        if (header == 0) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            structureIndices = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                structureIndices[ai] = (x >> 1) ^ -(x & 1);
            }
            header = buf[i++];
        }

        if (header == 1) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            positionOffsets = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                positionOffsets[ai] = (x >> 1) ^ -(x & 1);
            }
            header = buf[i++];
        }

        if (header == 2) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            positionData = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                positionData[ai] = (x >> 1) ^ -(x & 1);
            }
            header = buf[i++];
        }

        if (header == 3) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            operatorIndices = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                operatorIndices[ai] = (x >> 1) ^ -(x & 1);
            }
            header = buf[i++];
        }

        if (header == 4) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            operatorData = new String[length];
            for (int ai = 0; ai < length; ai++) {
                int size = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    size |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }

                int start = i;
                i += size;
                operatorData[ai] = new String(buf, start, size, StandardCharsets.US_ASCII);
            }
            header = buf[i++];
        }

        if (header != 0x7f) {
            throw new InputMismatchException("colfer: unknown header at byte " + (i - 1));
        }

        return new InvertedIndexBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
    }

    private void encodeInternal(ByteArrayOutputStream out, int[] structureIndices, int[] positionOffset, int[] positionData, int[] operatorIndices, String[] operatorData) throws IOException {
        if (structureIndices.length != 0) {
            out.write(0);
            writeIntArray(out, structureIndices);
        }
        if (positionOffset.length != 0) {
            out.write(1);
            writeIntArray(out, positionOffset);
        }
        if (positionData.length != 0) {
            out.write(2);
            writeIntArray(out, positionData);
        }
        if (operatorIndices.length != 0) {
            out.write(3);
            writeIntArray(out, operatorIndices);
        }
        if (operatorData.length != 0) {
            out.write(4);
            writeStringArray(out, operatorData);
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
            int x1 = v << 1 ^ v >> 31;
            while ((x1 & ~0x7f) != 0) {
                out.write(x1 | 0x80);
                x1 >>>= 7;
            }
            out.write(x1);
        }
    }

    private void writeStringArray(OutputStream out, String[] data) throws IOException {
        int x = data.length;

        while (x > 0x7f) {
            out.write(x | 0x80);
            x >>>= 7;
        }
        out.write(x);

        for (int ai = 0; ai < data.length; ai++) {
            String s = data[ai];
            if (s == null) {
                s = "";
                data[ai] = s;
            }

            int length = s.length();
            while (length > 0x7f) {
                out.write(length | 0x80);
                length >>>= 7;
            }
            out.write(length);

            for (int sIndex = 0, sLength = s.length(); sIndex < sLength; sIndex++) {
                char c = s.charAt(sIndex);
                if (c < '\u0080') {
                    out.write(c);
                } else {
                    throw new IllegalArgumentException("Non-ASCII characters aren't supported");
                }
            }
        }
    }

    @Override
    public ByteArrayOutputStream encode(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encodeInternal(outputStream, structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
        return outputStream;
    }
}
