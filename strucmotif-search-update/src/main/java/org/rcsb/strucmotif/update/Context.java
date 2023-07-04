package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures global state of the update process.
 */
public class Context implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    final List<UpdateItem> updateItems;
    final Set<StructureInformation> processed;
    final ThreadLocal<FileWriter> fileWriter;
    final List<FileWriter> globalFileWriterReferences;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     */
    public Context(List<UpdateItem> updateItems) {
        this.updateItems = updateItems;
        this.processed = Collections.synchronizedSet(new HashSet<>());
        this.fileWriter = new ThreadLocal<>();
        this.globalFileWriterReferences = new CopyOnWriteArrayList<>();
    }

    public FileWriter getFileWriter(StrucmotifConfig strucmotifConfig) throws IOException {
        FileWriter ref = fileWriter.get();
        if (ref != null) {
            return ref;
        }

        Path path = Paths.get(strucmotifConfig.getRootPath()).resolve(StrucmotifConfig.INDEX + Thread.currentThread().getId() + StrucmotifConfig.TMP_EXT);
        logger.info("Creating thread-specific index dump at {}", path);
        ref = new FileWriter(path.toFile());
        fileWriter.set(ref);
        globalFileWriterReferences.add(ref);
        return ref;
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing thread-specific index dumps");
        for (FileWriter fileWriter : globalFileWriterReferences) {
            fileWriter.close();
        }
    }
}
