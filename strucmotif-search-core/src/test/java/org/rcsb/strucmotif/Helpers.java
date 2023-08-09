package org.rcsb.strucmotif;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Helpers {
    public static final float DELTA = 0.001f;
    public static final float RELAXED_DELTA = 0.1f;

    public static short[] convertCoordsToShort(double[] array) {
        short[] out = new short[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (short) Math.round(array[i] * 10);
        }
        return out;
    }

    public static InputStream getOriginalBcif(String pdbId) {
        return getResource("orig/" + pdbId.toLowerCase() + ".bcif");
    }

    public static InputStream getRenumberedBcif(String pdbId) {
        return getResource("renum/" + pdbId + ".bcif");
    }

    public static InputStream getResource(String location) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        return Objects.requireNonNull(resourceAsStream, "failed to locate test resource: " + location);
    }

    public static byte[] convertEnumToByte(Enum<?>... array) {
        byte[] out = new byte[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) array[i].ordinal();
        }
        return out;
    }

    public static Path getResourceAsPath(String path) {
        return Paths.get(new File("src/test/resources/").getAbsolutePath()).resolve(path);
    }
}
