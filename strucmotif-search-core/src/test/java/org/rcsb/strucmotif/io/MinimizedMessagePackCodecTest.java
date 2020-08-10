package org.rcsb.strucmotif.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MinimizedMessagePackCodecTest {
    private final MessagePackCodec messagePackCodec = new MinimizedMessagePackCodec();

    @Test
    public void encodeString() {
        int[] expected = convertToIntArray("82 a2 53 31 a0 a2 53 32 ab 4c 6f 72 65 6d 20 69 70 73 75 6d");

        // {"S1":"","S2":"Lorem ipsum"}
        Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("S1", "");
        originalMap.put("S2", "Lorem ipsum");

        int[] actual = convertToIntArray(messagePackCodec.encode(originalMap));

        assertArrayEquals(expected, actual);
    }

    @Test
    public void encodeNumbers() {
        int[] expected = convertToIntArray("82 A2 49 31 11 A2 44 31 D0 BD");

        // {"I1": 17,"D1":-65.7}
        Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("I1", 17);
        originalMap.put("D1", -67);

        int[] actual = convertToIntArray(messagePackCodec.encode(originalMap));

        assertArrayEquals(expected, actual);
    }

    @Test
    public void encodeEmptyMap() {
        int[] expected = convertToIntArray("81 a3 6f 62 6a 80");

        // {"obj":{}}
        Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("obj", new LinkedHashMap<>());

        int[] actual = convertToIntArray(messagePackCodec.encode(originalMap));

        assertArrayEquals(expected, actual);
    }

    @Test
    public void encodeMap() {
        int[] expected = convertToIntArray("81 A3 6F 62 6A 83 A2 53 31 A3 73 74 72 A2 49 31 F4 A2 49 32 7F");

        // {"obj":{"S1":"str","I1":-12,"D1":127}}
        Map<String, Object> originalMap = new LinkedHashMap<>();
        Map<String, Object> internal = new LinkedHashMap<>();
        internal.put("S1", "str");
        internal.put("I1", -12);
        internal.put("I2", 127);
        originalMap.put("obj", internal);

        int[] actual = convertToIntArray(messagePackCodec.encode(originalMap));

        assertArrayEquals(expected, actual);
    }

    @Test
    public void encodeEmptyArray() {
        int[] expected = convertToIntArray("81 a3 61 72 72 90");

        // {"arr":[]}
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("arr", new Object[0]);

        int[] actual = convertToIntArray(messagePackCodec.encode(originalMap));

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testForward() throws IOException {
        // create test case
        Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("S1", "Lorem");
        originalMap.put("S2", "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor " +
                "invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.");
        originalMap.put("S3", "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor " +
                "invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo " +
                "duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit " +
                "amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt " +
                "ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo " +
                "dolores et ea rebum.");
        originalMap.put("S4", "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor " +
                "invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo " +
                "duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit " +
                "amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt " +
                "ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo " +
                "dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit " +
                "amet.");

        // encode by MessagePack
        byte[] packed = messagePackCodec.encode(originalMap);

        // decode by MessagePack
        Map<String, Object> unpacked = messagePackCodec.decode(new ByteArrayInputStream(packed));

        assertEquals(originalMap, unpacked);
    }

    private static int[] convertToIntArray(byte[] bytes) {
        int[] out = new int[bytes.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = bytes[i] & 0xFF;
        }
        return out;
    }

    private static int[] convertToIntArray(String hex) {
        return Pattern.compile(" ").splitAsStream(hex)
                .map(s -> "0x" + s)
                .mapToInt(Integer::decode)
                .toArray();
    }
}