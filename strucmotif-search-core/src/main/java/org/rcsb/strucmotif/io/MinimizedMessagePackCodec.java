package org.rcsb.strucmotif.io;

import com.google.inject.Singleton;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives room to further optimize the Mol* MessagePack codec. Many categories are not needed and could be omitted.
 * https://github.com/google/flatbuffers/blob/2706381eef639dc19eab2b823bc86d2b8521e230/java/com/google/flatbuffers/Utf8Safe.java#L73
 */
@Singleton
public class MinimizedMessagePackCodec implements MessagePackCodec {
    @Override
    public byte[] encodeArray(Object[] array) {
        return encodeInternal(array);
    }

    @Override
    public byte[] encode(Map<String, Object> input) {
        return encodeInternal(input);
    }

    private byte[] encodeInternal(Object object) {
        try {
            UnsynchronizedByteArrayOutputStream byteArrayOutputStream = new UnsynchronizedByteArrayOutputStream();
            UnsynchronizedDataOutputStream dataOutputStream = new UnsynchronizedDataOutputStream(byteArrayOutputStream);
            encodeInternal(object, dataOutputStream);
            dataOutputStream.flush();
            dataOutputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void encodeInternal(Object input, DataOutputStream stream) throws IOException {
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
            writeUTF(value, stream, length);
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
        if (input instanceof Integer) {
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

    private void writeUTF(String data, DataOutputStream stream, int length) throws IOException {
        int inIx = 0;
        for (; inIx < length; ++inIx) {
            stream.write(data.charAt(inIx));
        }
    }

    /* decoding */
    @Override
    public Object[] decodeArray(InputStream inputStream) throws IOException {
        Object[] array = (Object[]) decodeInternal(inputStream);
        inputStream.close();
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> decode(InputStream inputStream) throws IOException {
        Map<String, Object> map = (Map<String, Object>) decodeInternal(inputStream);
        inputStream.close();
        return map;
    }

    private Object decodeInternal(InputStream inputStream) throws IOException {
        final int int8 = inputStream.read();
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
            // uint8
            case 0xCC:
                return inputStream.read() & 0xFF;
            // uint16
            case 0xCD:
                return readShort(inputStream) & 0xFFFF;
            // uint32
            case 0xCE:
                return readUnsignedInt(inputStream);
            // int8
            case 0xD0:
                return inputStream.read();
            // int16
            case 0xD1:
                return readShort(inputStream);
            // int32
            case 0xD2:
                return readInt(inputStream);
            // str8
            case 0xD9:
                return str(inputStream, inputStream.read() & 0xFF);
            // str16
            case 0xDA:
                return str(inputStream, readShort(inputStream) & 0xFFFF);
            // str32
            case 0xDB:
                return str(inputStream, readUnsignedInt(inputStream));
            // array16
            case 0xDC:
                return array(inputStream, readShort(inputStream) & 0xFFFF);
            // array32
            case 0xDD:
                return array(inputStream, readUnsignedInt(inputStream));
            // map16
            case 0xDE:
                return map(inputStream, readShort(inputStream) & 0xFFFF);
            // map32
            case 0xDF:
                return map(inputStream, readUnsignedInt(inputStream));
        }

        throw new IllegalArgumentException("Unknown MessagePack type 0x" + type);
    }

    private int readUnsignedInt(InputStream inputStream) throws IOException {
        return (int) (readInt(inputStream) & 0xFFFFFFFFL);
    }

    private int readShort(InputStream inputStream) throws IOException {
        int ch1 = inputStream.read();
        int ch2 = inputStream.read();
        return ((ch1 << 8) + ch2);
    }

    private int readInt(InputStream inputStream) throws IOException {
        int ch1 = inputStream.read();
        int ch2 = inputStream.read();
        int ch3 = inputStream.read();
        int ch4 = inputStream.read();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
    }

    private Map<String, Object> map(InputStream inputStream, int length) throws IOException {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int i = 0; i < length; i++) {
            value.put((String) decodeInternal(inputStream), decodeInternal(inputStream));
        }
        return value;
    }

    private byte[] bin(InputStream inputStream, int length) throws IOException {
        byte[] bin = new byte[length];
        int inIx = 0;
        for (; inIx < length; ++inIx) {
            bin[inIx] = (byte) inputStream.read();
        }
        return bin;
    }

    private String str(InputStream inputStream, int length) throws IOException {
        return new String(bin(inputStream, length));
    }

    private Object[] array(InputStream inputStream, int length) throws IOException {
        Object[] value = new Object[length];
        for (int i = 0; i < length; i++) {
            value[i] = decodeInternal(inputStream);
        }
        return value;
    }
}
