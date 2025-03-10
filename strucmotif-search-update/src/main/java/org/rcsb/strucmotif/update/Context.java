package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
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
    private final Map<String, OutputStream> outputStreams;
    private final List<Path> outputPaths;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     * @param strucmotifConfig global config
     * @param updateItems update list
     */
    public Context(StrucmotifConfig strucmotifConfig, List<UpdateItem> updateItems) {
        this.rootPath = strucmotifConfig.getRootPath();
        this.updateItems = updateItems;
        this.processed = Collections.synchronizedSet(new HashSet<>());
        this.outputStreams = Collections.synchronizedMap(new HashMap<>());
        this.outputPaths = new CopyOnWriteArrayList<>();
    }

    private String getPrefix(int descriptor) {
        return ResiduePairDescriptor.getResidueType1(descriptor).getInternalCode() + ResiduePairDescriptor.getResidueType2(descriptor).getInternalCode();
    }

    /**
     * Get an output stream that a thread can use to dump data.
     * @param descriptor the descriptor to write
     * @return a new or previously created output stream that is specific to this thread
     * @throws IOException IO operation failed
     */
    public OutputStream getOutputStream(int descriptor) throws IOException {
        String key = Thread.currentThread().getId() + "-" + getPrefix(descriptor);
        OutputStream ref = outputStreams.get(key);
        if (ref != null) {
            return ref;
        }

        Path path = Paths.get(rootPath).resolve(StrucmotifConfig.INDEX + "." + key + StrucmotifConfig.TMP_EXT);
        logger.debug("Creating thread-specific index dump at {}", path);
        ref = new BufferedOutputStream(new FileOutputStream(path.toFile()), BUFFER_SIZE);
        outputStreams.put(key, ref);
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
