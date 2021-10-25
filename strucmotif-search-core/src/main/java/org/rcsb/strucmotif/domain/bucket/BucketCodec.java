package org.rcsb.strucmotif.domain.bucket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BucketCodec {
    public static InvertedIndexBucket decode(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        InvertedIndexBucket bucket = decodeInternal(dataInputStream);
        dataInputStream.close();
        return bucket;
    }

    private static InvertedIndexBucket decodeInternal(DataInputStream inputStream) throws IOException {
        int[] structureIndices = decodeIntArray(inputStream);
        int[] positionOffsets = decodeIntArray(inputStream);
        int[] positionData = decodeIntArray(inputStream);
        int[] operatorIndices = decodeIntArray(inputStream);
        String[] operatorData = decodeStringArray(inputStream);
        return new InvertedIndexBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
    }

    private static int[] decodeIntArray(DataInputStream inputStream) throws IOException {
        int[] out = new int[readArrayLength(inputStream)];
        for (int i = 0; i < out.length; i++) {
            out[i] = readInt(inputStream);
        }
        return out;
    }

    private static int readInt(DataInputStream inputStream) throws IOException {
        final int int8 = inputStream.readByte();
        final int type = int8 & 0xFF;

        // positive FixInt
        if ((type & 0x80) == 0x00) {
            return type;
        }

        // negative FixInt
        if ((type & 0xE0) == 0xE0) {
            return int8;
        }

        switch (type) {
            // uint8
            case 0xCC:
                return inputStream.readByte() & 0xFF;
            // uint16
            case 0xCD:
                return inputStream.readShort() & 0xFFFF;
            // uint32
            case 0xCE:
                return readUnsignedInt(inputStream);
            // int8
            case 0xD0:
                return inputStream.readByte();
            // int16
            case 0xD1:
                return inputStream.readShort();
            // int32
            case 0xD2:
                return inputStream.readInt();
        }

        throw new IllegalArgumentException("Unknown MessagePack type 0x" + Integer.toHexString(type) + ", expected a int here!");
    }

    private static String[] decodeStringArray(DataInputStream inputStream) throws IOException {
        String[] out = new String[readArrayLength(inputStream)];
        for (int i = 0; i < out.length; i++) {
            out[i] = readString(inputStream);
        }
        return out;
    }

    private static String readString(DataInputStream inputStream) throws IOException {
        byte[] bytes = new byte[readStringLength(inputStream)];
        inputStream.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static int readStringLength(DataInputStream inputStream) throws IOException {
        final int int8 = inputStream.readByte();
        final int type = int8 & 0xFF;

        // FixStr
        if ((type & 0xE0) == 0xA0) {
            return type & 0x1F;
        }

        switch (type) {
            // str8
            case 0xD9:
                return inputStream.readByte() & 0xFF;
            // str16
            case 0xDA:
                return inputStream.readShort() & 0xFFFF;
            // str32
            case 0xDB:
                return readUnsignedInt(inputStream);
        }

        throw new IllegalArgumentException("Unexpected MessagePack type 0x" + Integer.toHexString(type) + ", expected a StringArray here!");
    }

    private static int readArrayLength(DataInputStream inputStream) throws IOException {
        final int int8 = inputStream.readByte();
        final int type = int8 & 0xFF;

        if ((type & 0xF0) == 0x90) { // FixArray
            return type & 0x0F;
        } else if (type == 0xDC) { // Array16
            return inputStream.readShort() & 0xFFFF;
        } else if (type == 0xDD) { // Array32
            return readUnsignedInt(inputStream);
        } else {
            throw new IllegalArgumentException("Unexpected MessagePack type 0x" + Integer.toHexString(type) + ", expected array length here!");
        }
    }

    private static int readUnsignedInt(DataInputStream inputStream) throws IOException {
        return (int) (inputStream.readInt() & 0xFFFFFFFFL);
    }

    public static ByteArrayOutputStream encode(ResiduePairIdentifierBucket bucket) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        encodeInternal(bucket, dataOutputStream);
        dataOutputStream.flush();
        dataOutputStream.close();
        return byteArrayOutputStream;
    }

    private static void encodeInternal(Bucket bucket, DataOutputStream outputStream) throws IOException {
        int structureCount = bucket.getStructureCount();
        int[] structureIndices = new int[structureCount];
        int[] positionOffsets = new int[structureCount];
        int[] positionData = new int[bucket.getResiduePairCount() * 2];
        List<Integer> operatorIndicesList = new ArrayList<>();
        List<String> operatorDataList = new ArrayList<>();

        int structurePointer = 0;
        int positionPointer = 0;
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            structureIndices[structurePointer] = bucket.getStructureIndex();
            positionOffsets[structurePointer] = positionPointer;

            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();
                positionData[positionPointer] = bucket.getIndex1();
                positionData[positionPointer + 1] = bucket.getIndex2();

                String structOperId1 = bucket.getStructOperId1();
                String structOperId2 = bucket.getStructOperId2();
                if (!structOperId1.equals(Bucket.DEFAULT_OPERATOR)) {
                    operatorIndicesList.add(positionPointer);
                    operatorDataList.add(structOperId1);
                }
                if (!structOperId2.equals(Bucket.DEFAULT_OPERATOR)) {
                    operatorIndicesList.add(positionPointer + 1);
                    operatorDataList.add(structOperId2);
                }

                positionPointer += 2;
            }

            structurePointer++;
        }

        int[] operatorIndices = operatorIndicesList.stream().mapToInt(Integer::intValue).toArray();
        String[] operatorData = operatorDataList.toArray(String[]::new);

        encodeIntArray(structureIndices, outputStream);
        encodeIntArray(positionOffsets, outputStream);
        encodeIntArray(positionData, outputStream);
        encodeIntArray(operatorIndices, outputStream);
        encodeStringArray(operatorData, outputStream);
    }

    private static void encodeIntArray(int[] value, DataOutputStream outputStream) throws IOException {
        writeArrayLength(value.length, outputStream);
        for (int v : value) {
            writeInt(v, outputStream);
        }
    }

    private static void encodeStringArray(String[] value, DataOutputStream outputStream) throws IOException {
        writeArrayLength(value.length, outputStream);
        for (String v : value) {
            writeString(v, outputStream);
        }
    }

    private static void writeArrayLength(int length, DataOutputStream outputStream) throws IOException {
        if (length < 0x10) {
            outputStream.writeByte(length | 0x90);
        } else if (length < 0x10000) {
            outputStream.writeByte(0xDC);
            outputStream.writeShort(length);
        } else {
            outputStream.writeByte(0xDD);
            outputStream.writeInt(length);
        }
    }

    private static void writeInt(int value, DataOutputStream outputStream) throws IOException {
        if (value >= 0) {
            // positive fixnum
            if (value < 0x80) {
                outputStream.writeByte(value);
                // uint 8
            } else if (value < 0x100) {
                outputStream.writeByte(0xCC);
                outputStream.writeByte(value);
                // uint 16
            } else if (value < 0x10000) {
                outputStream.writeByte(0xCD);
                outputStream.writeShort(value);
                // uint 32
            } else {
                outputStream.writeByte(0xCE);
                outputStream.writeInt(value);
            }
            return;
        }

        // negative fixnum
        if (value >= -0x20) {
            outputStream.writeByte(value);
            // int 8
        } else if (value >= -0x80) {
            outputStream.writeByte(0xD0);
            outputStream.writeByte(value);
            // int 16
        } else if (value >= -0x8000) {
            outputStream.writeByte(0xD1);
            outputStream.writeShort(value);
            // int 32
        } else {
            outputStream.writeByte(0xD2);
            outputStream.writeInt(value);
        }
    }

    private static void writeString(String value, DataOutputStream outputStream) throws IOException {
        int length = value.length();
        // fix str
        if (length < 0x20) {
            outputStream.writeByte(length | 0xA0);
            // str 8
        } else if (length < 0x100) {
            outputStream.writeByte(0xD9);
            outputStream.writeByte(length);
            // str 16
        } else if (length < 0x10000) {
            outputStream.writeByte(0xDA);
            outputStream.writeShort(length);
            // str 32
        } else {
            outputStream.writeByte(0xDB);
            outputStream.writeInt(length);
        }
        outputStream.write(value.getBytes(StandardCharsets.US_ASCII));
    }
}
