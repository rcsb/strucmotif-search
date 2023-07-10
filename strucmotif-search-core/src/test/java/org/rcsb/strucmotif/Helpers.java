package org.rcsb.strucmotif;

import org.rcsb.strucmotif.config.StrucmotifConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Collectors;

public class Helpers {
    public static final float DELTA = 0.001f;
    public static final float RELAXED_DELTA = 0.1f;

    public static void main(String[] args) throws IOException {
        // use this to update inverted index files
        printTestIdentifiers();
        copyTestFiles(); // TODO update
    }

    private static void printTestIdentifiers() {
        InputStream inputStream = getResource("known.list");
        System.out.println(new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .map(line -> line.split(",")[0])
                .collect(Collectors.joining(" ")));
        System.out.println();
    }

    private static void copyTestFiles() throws IOException {
        Path dataRoot = Paths.get("/opt/data-2/");
        Path projectRoot = Paths.get("/Users/sebastian/IdeaProjects/strucmotif-search/");
        Path resourcePath = projectRoot.resolve("strucmotif-search-core").resolve("src").resolve("test").resolve("resources");

        Files.copy(dataRoot.resolve(StrucmotifConfig.STATE_KNOWN_LIST), resourcePath.resolve(StrucmotifConfig.STATE_KNOWN_LIST), StandardCopyOption.REPLACE_EXISTING);

        Files.list(dataRoot.resolve(StrucmotifConfig.RENUMBERED))
                .filter(p -> p.toFile().getName().endsWith(".bcif.gz"))
                .forEach(source -> {
                    try {
                        String name = source.toFile().getName();
                        Path destination = resourcePath.resolve("renum").resolve(name);
                        System.out.println(source + " -> " + destination);
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        Files.list(resourcePath.resolve("index"))
                .filter(p -> p.toFile().getName().endsWith(".colf"))
                .forEach(destination -> {
                    try {
                        String name = destination.toFile().getName();
                        Path source = dataRoot.resolve(StrucmotifConfig.INDEX).resolve(name.substring(0, 2)).resolve(name);
                        System.out.println(source + " -> " + destination);
                        Files.copy(source, destination.getParent().resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

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
