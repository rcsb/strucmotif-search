package org.rcsb.strucmotif;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.DefaultStructureWriter;
import org.rcsb.strucmotif.io.StructureWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class UpdateTestData {
    private static final Path updateRoot;
    private static final StrucmotifConfig strucmotifConfig;
    private static final StructureWriter structureWriter;

    static {
        Path root = Paths.get(".").toAbsolutePath();
        System.out.println("Project path: " + root);
        updateRoot = root.resolve("strucmotif-search-core/src/test/resources/");
        System.out.println("Update root: " + updateRoot);
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setRootPath(updateRoot.toString());
        structureWriter = new DefaultStructureWriter(new DefaultResidueTypeResolver(strucmotifConfig), strucmotifConfig);
    }

    public static void main(String[] args) throws IOException {
        updateOriginalBcif(updateRoot.resolve("orig"));
        updateRenumberedBcif(updateRoot.resolve("renum"));
    }

    private static void updateOriginalBcif(Path path) throws IOException {
        try (Stream<Path> paths = Files.list(path)) {
            paths.forEach(p -> {
                String fileName = p.getFileName().toString();
                System.out.println("Updating original: " + fileName);
                download("https://models.rcsb.org/" + fileName, p);
            });
        }
    }

    private static void updateRenumberedBcif(Path path) throws IOException {
        try (Stream<Path> paths = Files.list(path)) {
            paths.forEach(p -> {
                String fileName = p.getFileName().toString();
                System.out.println("Updating renumbered: " + fileName);

                try {
                    MmCifFile cif = CifIO.readFromURL(new URL("https://models.rcsb.org/" + fileName)).as(StandardSchemata.MMCIF);
                    byte[] bytes = structureWriter.write(cif);
                    Files.write(p, bytes);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static void download(String sourceUrl, Path dest) {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(sourceUrl).openStream());
             FileOutputStream outputStream = new FileOutputStream(dest.toFile());
             FileChannel outputChannel = outputStream.getChannel()) {
            outputChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
