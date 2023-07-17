package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures global state of the update process.
 */
public class Context implements Closeable, Flushable {
    private static final Logger logger = LoggerFactory.getLogger(Context.class);
    private static final int BUFFER_SIZE = 65536;
    private final String rootPath;
    final List<UpdateItem> updateItems;
    final Set<StructureInformation> processed;
    int partitionSize;
    String partitionContext;
    private final Map<Thread, OutputStream> outputStreams;
    private final List<Path> outputPaths;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     */
    public Context(StrucmotifConfig strucmotifConfig, List<UpdateItem> updateItems) {
        this.rootPath = strucmotifConfig.getRootPath();
        this.updateItems = updateItems;
        this.processed = Collections.synchronizedSet(new HashSet<>());
        this.outputStreams = Collections.synchronizedMap(new HashMap<>());
        this.outputPaths = new CopyOnWriteArrayList<>();
    }

    public OutputStream getOutputStream() throws IOException {
        OutputStream ref = outputStreams.get(Thread.currentThread());
        if (ref != null) {
            return ref;
        }

        Path path = Paths.get(rootPath).resolve(StrucmotifConfig.INDEX + "." + Thread.currentThread().getId() + StrucmotifConfig.TMP_EXT);
        logger.debug("Creating thread-specific index dump at {}", path);
        ref = new BufferedOutputStream(new FileOutputStream(path.toFile()), BUFFER_SIZE);
        outputStreams.put(Thread.currentThread(), ref);
        outputPaths.add(path);
        return ref;
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream stream : outputStreams.values()) {
            stream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing thread-specific index dumps");
        for (OutputStream stream : outputStreams.values()) {
            stream.close();
        }
        outputStreams.clear();
        for (Path path : outputPaths) {
            Files.deleteIfExists(path);
        }
        outputPaths.clear();
    }
}
