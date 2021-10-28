package org.rcsb.strucmotif.domain.bucket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;

/**
 * Data bean with built-in serialization support.
 * @see <a href="https://github.com/pascaldekloe/colfer">Colfer's home</a>
 */
public class ColferBucket {
    private int[] structureIndices;
    private int[] positionOffsets;
    private int[] positionData;
    private int[] operatorIndices;
    private String[] operatorData;

    private static final int[] _zeroStructureIndices = new int[0];
    private static final int[] _zeroPositionOffsets = new int[0];
    private static final int[] _zeroPositionData = new int[0];
    private static final int[] _zeroOperatorIndices = new int[0];
    private static final String[] _zeroOperatorData = new String[0];

    /** Default constructor */
    public ColferBucket() {
        this.structureIndices = _zeroStructureIndices;
        this.positionOffsets = _zeroPositionOffsets;
        this.positionData = _zeroPositionData;
        this.operatorIndices = _zeroOperatorIndices;
        this.operatorData = _zeroOperatorData;
    }

    public ColferBucket(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
        this.structureIndices = structureIndices;
        this.positionOffsets = positionOffsets;
        this.positionData = positionData;
        this.operatorIndices = operatorIndices;
        this.operatorData = operatorData;
    }

    /**
     * Serializes the object.
     * All {@code null} elements in {@link #structureIndices} will be replaced with a {@code new} value.
     * All {@code null} elements in {@link #positionOffsets} will be replaced with a {@code new} value.
     * All {@code null} elements in {@link #positionData} will be replaced with a {@code new} value.
     * All {@code null} elements in {@link #operatorIndices} will be replaced with a {@code new} value.
     * All {@code null} elements in {@link #operatorData} will be replaced with {@code ""}.
     * @param out the data destination.
     * @throws IOException from {@code out}.
     */
    public void marshal(OutputStream out) throws IOException {
        if (structureIndices.length != 0) {
            out.write(0);
            writeIntArray(out, structureIndices);
        }

        if (positionOffsets.length != 0) {
            out.write(1);
            writeIntArray(out, positionOffsets);
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
            String[] a = operatorData;

            int x = a.length;

            while (x > 0x7f) {
                out.write(x | 0x80);
                x >>>= 7;
            }
            out.write(x);

            for (int ai = 0; ai < a.length; ai++) {
                String s = a[ai];
                if (s == null) {
                    s = "";
                    a[ai] = s;
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

    /**
     * Deserializes the object.
     * @param buf the data source.
     */
    public void unmarshal(byte[] buf) {
        int i = 0;
        byte header = buf[i++];

        if (header == 0) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            int[] a = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                a[ai] = (x >> 1) ^ -(x & 1);
            }
            structureIndices = a;
            header = buf[i++];
        }

        if (header == 1) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            int[] a = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                a[ai] = (x >> 1) ^ -(x & 1);
            }
            positionOffsets = a;
            header = buf[i++];
        }

        if (header == 2) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            int[] a = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                a[ai] = (x >> 1) ^ -(x & 1);
            }
            positionData = a;
            header = buf[i++];
        }

        if (header == 3) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            int[] a = new int[length];
            for (int ai = 0; ai < length; ai++) {
                int x = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    x |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }
                a[ai] = (x >> 1) ^ -(x & 1);
            }
            operatorIndices = a;
            header = buf[i++];
        }

        if (header == 4) {
            int length = 0;
            for (int shift = 0; true; shift += 7) {
                byte b = buf[i++];
                length |= (b & 0x7f) << shift;
                if (shift == 28 || b >= 0) break;
            }

            String[] a = new String[length];
            for (int ai = 0; ai < length; ai++) {
                int size = 0;
                for (int shift = 0; true; shift += 7) {
                    byte b = buf[i++];
                    size |= (b & 0x7f) << shift;
                    if (shift == 28 || b >= 0) break;
                }

                int start = i;
                i += size;
                a[ai] = new String(buf, start, size, StandardCharsets.US_ASCII);
            }
            operatorData = a;
            header = buf[i++];
        }

        if (header != 0x7f)
            throw new InputMismatchException("colfer: unknown header at byte " + (i - 1));
    }

    /**
     * Gets org.rcsb.strucmotif.model/test.bucket.structureIndices.
     * @return the value.
     */
    public int[] getStructureIndices() {
        return structureIndices;
    }

    /**
     * Gets org.rcsb.strucmotif.model/test.bucket.positionOffsets.
     * @return the value.
     */
    public int[] getPositionOffsets() {
        return positionOffsets;
    }

    /**
     * Gets org.rcsb.strucmotif.model/test.bucket.positionData.
     * @return the value.
     */
    public int[] getPositionData() {
        return positionData;
    }

    /**
     * Gets org.rcsb.strucmotif.model/test.bucket.operatorIndices.
     * @return the value.
     */
    public int[] getOperatorIndices() {
        return operatorIndices;
    }

    /**
     * Gets org.rcsb.strucmotif.model/test.bucket.operatorData.
     * @return the value.
     */
    public String[] getOperatorData() {
        return operatorData;
    }
}
