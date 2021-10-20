package org.rcsb.strucmotif.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decodes and encodes MessagePack data.
 */
class InvertedIndexCodec {
    public static ByteArrayOutputStream encode(Map<?, ?> input) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            encodeInternal(input, dataOutputStream);
            dataOutputStream.flush();
            dataOutputStream.close();

            return byteArrayOutputStream;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void encodeInternal(Object input, DataOutputStream stream) throws IOException {
        // string bytes
        if (input instanceof String) {
            String value = (String) input;
            int length = value.length();
            // fix str
            if (length < 0x20) {
                stream.writeByte(length | 0xA0);
                // str 8
            } else if (length < 0x100) {
                stream.writeByte(0xD9);
                stream.writeByte(length);
                // str 16
            } else if (length < 0x10000) {
                stream.writeByte(0xDA);
                stream.writeShort(length);
                // str 32
            } else {
                stream.writeByte(0xDB);
                stream.writeInt(length);
            }
            stream.write(value.getBytes(StandardCharsets.US_ASCII));
            return;
        }

        // byte arrays
        if (input instanceof byte[]) {
            byte[] value = (byte[]) input;
            int length = value.length;
            // bin 8
            if (length < 0x100) {
                stream.writeByte(0xC4);
                stream.writeByte(length);
                // bin 16
            } else if (length < 0x10000) {
                stream.writeByte(0xC5);
                stream.writeShort(length);
                // bin 32
            } else {
                stream.writeByte(0xC6);
                stream.writeInt(length);
            }
            stream.write(value);
            return;
        }

        // numbers
        if (input instanceof Integer || input instanceof Double) {
            if (input instanceof Double && Double.isInfinite((double) input)) {
                throw new IllegalArgumentException("Number not finite: " + input);
            }

            // double
            if (input instanceof Double) {
                double value = (double) input;
                stream.writeByte(0xCB);
                stream.writeDouble(value);
                return;
            }

            // int
            int value = (int) input;
            if (value >= 0) {
                // positive fixnum
                if (value < 0x80) {
                    stream.writeByte(value);
                    // uint 8
                } else if (value < 0x100) {
                    stream.writeByte(0xCC);
                    stream.writeByte(value);
                    // uint 16
                } else if (value < 0x10000) {
                    stream.writeByte(0xCD);
                    stream.writeShort(value);
                    // uint 32
                } else {
                    stream.writeByte(0xCE);
                    stream.writeInt(value);
                }
                return;
            }
            // negative fixnum
            if (value >= -0x20) {
                stream.writeByte(value);
                // int 8
            } else if (value >= -0x80) {
                stream.writeByte(0xD0);
                stream.writeByte(value);
                // int 16
            } else if (value >= -0x8000) {
                stream.writeByte(0xD1);
                stream.writeShort(value);
                // int 32
            } else {
                stream.writeByte(0xD2);
                stream.writeInt(value);
            }
            return;
        }

        // null
        if (input == null) {
            stream.writeByte(0xC0);
            return;
        }

        // boolean
        if (input instanceof Boolean) {
            boolean value = (boolean) input;
            stream.writeByte(value ? 0xC3 : 0xC2);
            return;
        }

        // Container Types
        int length;
        boolean isArray = input.getClass().isArray();

        if (isArray) {
            length = ((Object[]) input).length;
        } else {
            length = ((Map<?, ?>) input).size();
        }

        if (length < 0x10) {
            stream.writeByte(length | (isArray ? 0x90 : 0x80));
        } else if (length < 0x10000) {
            stream.writeByte(isArray ? 0xDC : 0xDE);
            stream.writeShort(length);
        } else {
            stream.writeByte(isArray ? 0xDD : 0xDF);
            stream.writeInt(length);
        }

        if (isArray) {
            Object[] value = (Object[]) input;
            for (Object object : value) {
                encodeInternal(object, stream);
            }
        } else {
            Map<?, ?> value = (Map<?, ?>) input;
            for (Object key : value.keySet()) {
                encodeInternal(key, stream);
                encodeInternal(value.get(key), stream);
            }
        }
    }

    public static Map<?, ?> decode(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        Map<?, ?> map = (Map<?, ?>) decodeInternal(dataInputStream);
        dataInputStream.close();
        return map;
    }

    private static Object decodeInternal(DataInputStream inputStream) throws IOException {
        final int int8 = inputStream.readByte();
        final int type = int8 & 0xFF;

        // positive FixInt
        if ((type & 0x80) == 0x00) {
            return type;
        }

        // FixMap
        if ((type & 0xF0) == 0x80) {
            return map(inputStream, type & 0x0F);
        }

        // FixArray
        if ((type & 0xF0) == 0x90) {
            return array(inputStream, type & 0x0F);
        }

        // FixStr
        if ((type & 0xE0) == 0xA0) {
            return str(inputStream, type & 0x1F);
        }

        // negative FixInt
        if ((type & 0xE0) == 0xE0) {
            return int8;
        }

        switch (type) {
            // null
            case 0xC0:
                return null;
            // false
            case 0xC2:
                return false;
            // true
            case 0xC3:
                return true;
            // bin8
            case 0xC4:
                return bin(inputStream, inputStream.read() & 0xFF);
            // bin16
            case 0xC5:
                return bin(inputStream, inputStream.readShort() & 0xFFFF);
            // bin32
            case 0xC6:
                return bin(inputStream, readUnsignedInt(inputStream));
            // float32
            case 0xCA:
                return (double) inputStream.readFloat();
            // float64
            case 0xCB:
                return inputStream.readDouble();
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
                return (int) inputStream.readByte();
            // int16
            case 0xD1:
                return (int) inputStream.readShort();
            // int32
            case 0xD2:
                return inputStream.readInt();
            // str8
            case 0xD9:
                return str(inputStream, inputStream.readByte() & 0xFF);
            // str16
            case 0xDA:
                return str(inputStream, inputStream.readShort() & 0xFFFF);
            // str32
            case 0xDB:
                return str(inputStream, readUnsignedInt(inputStream));
            // array16
            case 0xDC:
                return array(inputStream, inputStream.readShort() & 0xFFFF);
            // array32
            case 0xDD:
                return array(inputStream, readUnsignedInt(inputStream));
            // map16
            case 0xDE:
                return map(inputStream, inputStream.readShort() & 0xFFFF);
            // map32
            case 0xDF:
                return map(inputStream, readUnsignedInt(inputStream));
        }

        throw new IllegalArgumentException("Unknown MessagePack type 0x" + type);
    }

    private static int readUnsignedInt(DataInputStream inputStream) throws IOException {
        return (int) (inputStream.readInt() & 0xFFFFFFFFL);
    }

    private static Map<?, ?> map(DataInputStream inputStream, int length) throws IOException {
        Map<Object, Object> value = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            value.put(decodeInternal(inputStream), decodeInternal(inputStream));
        }
        return value;
    }

    private static byte[] bin(DataInputStream inputStream, int length) throws IOException {
        byte[] tmp = new byte[length];
        inputStream.readFully(tmp);
        return tmp;
    }

    private static String str(DataInputStream inputStream, int length) throws IOException {
        return new String(bin(inputStream, length), StandardCharsets.US_ASCII);
    }

    private static Object[] array(DataInputStream inputStream, int length) throws IOException {
        final Object[] value = new Object[length];
        for (int i = 0; i < length; i++) {
            value[i] = decodeInternal(inputStream);
        }
        return value;
    }
}

