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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures global state of the update process.
 */
public class Context implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    private final String rootPath;
    final List<UpdateItem> updateItems;
    final Set<StructureInformation> processed;
    private final Map<Thread, Map<Integer, FileWriter>> fileWriterReferences;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     */
    public Context(StrucmotifConfig strucmotifConfig, List<UpdateItem> updateItems) {
        this.rootPath = strucmotifConfig.getRootPath();
        this.updateItems = updateItems;
        this.processed = Collections.synchronizedSet(new HashSet<>());
        this.fileWriterReferences = new HashMap<>();
    }

    public FileWriter getFileWriter(int residuePairDescriptor) throws IOException {
        Map<Integer, FileWriter> map = fileWriterReferences.computeIfAbsent(Thread.currentThread(), e -> Collections.synchronizedMap(new HashMap<>()));
        FileWriter ref = map.get(residuePairDescriptor);
        if (ref != null) {
            return ref;
        }

        Path path = Paths.get(rootPath).resolve(StrucmotifConfig.INDEX + "-" + Thread.currentThread().getId() + "-" + residuePairDescriptor + StrucmotifConfig.TMP_EXT);
        logger.info("Creating thread-specific index dump at {}", path);
        ref = new FileWriter(path.toFile());
        map.put(residuePairDescriptor, ref);
        return ref;
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing thread-specific index dumps");
        for (Map<Integer, FileWriter> fileWriters : fileWriterReferences.values()) {
            for (FileWriter fileWriter : fileWriters.values()) {
                fileWriter.close();
            }
        }
        fileWriterReferences.clear();
    }
}
