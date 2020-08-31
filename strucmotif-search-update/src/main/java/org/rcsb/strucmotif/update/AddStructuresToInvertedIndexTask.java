package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.ResidueGraph;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Routine to add new structures to the structural motif search tool. Single argument is an array of PDB-identifiers to
 * process. The array may be empty (causing a no-operation).
 * <p>
 * This will:
 * <ul>
 *     <li>creating 'optimized' binary structure files for all structures</li>
 *     <li>determining all words (i.e. pairs of components) and adding them to the inverted index</li>
 *     <li>keep track of the ids of all 'registered' files in the optimized archive and in the index</li>
 * </ul>
 * <p>
 * Implementation details: Files will be obtained on-the-fly from <code>models.rcsb.org</code>. That means that this
 * step strictly requires that the binary CIF creation during update did finish. Touching the files of the inverted
 * index is costly. Therefore, this update step 'buffers' results for a number of structures before 'flushing'
 * everything to the file system. Default batch size is 400.
 * <p>
 * Risks:
 * <ul>
 *     <li>Stopping or a (JVM)-crash during writing to file system will most likely compromise the whole index - you
 *     will experience MessagePack acting up as a consequence (caused by fixed length arrays ending prematurely). There
 *     is no obvious way to recover, so backup/snapshot accordingly.</li>
 *     <li>Implementation is using all available resources of the common thread pool.</li>
 *     <li>Writing to file system is done in parallel (no file is touched twice during 1 write operation, though).</li>
 *     <li>Be aware that some structures will compute for a considerable time (up to several hours) and will block
 *     flushing of all other threads.</li>
 * </ul>
 */
public class AddStructuresToInvertedIndexTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToInvertedIndexTask.class);
    private static final int CHUNK_SIZE = 400;
    private static final String TASK_NAME = AddStructuresToInvertedIndexTask.class.getSimpleName();
    private final InvertedIndex motifLookup;
    private final RenumberedReader renumberedReader;

    public AddStructuresToInvertedIndexTask(String[] args, InvertedIndex motifLookup, RenumberedReader renumberedReader) throws IOException {
        this.motifLookup = motifLookup;
        this.renumberedReader = renumberedReader;

        logger.info("[{}] Starting structural motif search index update",
                TASK_NAME);

        List<String> identifiers = Arrays.stream(args).collect(Collectors.toList());
        // we shuffle because certain 'troublemakers' (e.g. ribosomes or virus capsids) appear close together, in a full update this leads to 1 bin maxing out available heap space
        Collections.shuffle(identifiers);

        // create respectively ensure directories exist
        motifLookup.createDirectories();

        // we assume that the argument list does not contain any identifiers already present in the index
        // work on optimized path so that component index mapping is valid
        List<Path> paths = identifiers.stream()
                .map(id -> MotifSearch.ARCHIVE_PATH.resolve(id + ".bcif"))
                .collect(Collectors.toList());

        long totalFileCount = paths.size();
        logger.info("[{}] {} files to process in total",
                TASK_NAME,
                totalFileCount);

        Partition<Path> partitions = new Partition<>(paths, CHUNK_SIZE);
        logger.info("[{}] Formed {} partitions",
                TASK_NAME,
                partitions.size());

        Context context = new Context();
        context.processed = Collections.synchronizedSet(new HashSet<>());

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            context.partitionContext = (i + 1) + " / " + partitions.size();
            logger.info("[{}] Start processing partition",
                    context.partitionContext);

            context.structureCounter = new AtomicInteger();
            context.buffer = new ConcurrentHashMap<>();
            partitions.get(i)
                    .parallelStream()
                    .forEach(path -> detectMotifs(path, context));

            writeMotifs(context);
        }

        logger.info("[{}] Finished index update",
                TASK_NAME);
    }

    static class Context {
        Set<String> processed;
        String partitionContext;
        Map<ResiduePairDescriptor, Map<String, List<ResiduePairIdentifier>>> buffer;
        AtomicInteger structureCounter;
    }

    private void detectMotifs(Path path, Context context) {
        String structureIdentifier = path.toFile().getName().split("\\.")[0];
        int count = context.structureCounter.incrementAndGet();
        String structureContext = count + " / " + CHUNK_SIZE + "] [" + structureIdentifier;

        // fails when file is missing (should not happen) or does not contain valid polymer chain
        Structure structure;
        try {
            structure = renumberedReader.readById(structureIdentifier);
        } catch (UncheckedIOException e) {
            logger.warn("[{}] [{}] Source file missing unexpectedly",
                    context.partitionContext,
                    structureContext,
                    e);
            return;
        } catch (UnsupportedOperationException e) {
            logger.warn("[{}] [{}] No valid polymer chains",
                    context.partitionContext,
                    structureContext);
            return;
        }

        try {
            ResidueGraph residueGraph = new ResidueGraph(structure);

            // extract motifs
            AtomicInteger structureMotifCounter = new AtomicInteger();
            residueGraph.residuePairOccurrencesParallel()
                    .forEach(motifOccurrence -> {
                        ResiduePairDescriptor motifDescriptor = motifOccurrence.getResiduePairDescriptor();
                        ResiduePairIdentifier targetIdentifier = motifOccurrence.getResidueIdentifier();

                        Map<String, List<ResiduePairIdentifier>> groupedTargetIdentifiers = context.buffer.computeIfAbsent(motifDescriptor, k -> Collections.synchronizedMap(new HashMap<>()));
                        List<ResiduePairIdentifier> targetIdentifiers = groupedTargetIdentifiers.computeIfAbsent(structureIdentifier, k -> Collections.synchronizedList(new ArrayList<>()));
                        targetIdentifiers.add(targetIdentifier);
                        structureMotifCounter.incrementAndGet();
                    });
            logger.info("[{}] [{}] Extracted {} words",
                    context.partitionContext,
                    structureContext,
                    structureMotifCounter.get());
            context.processed.add(structureIdentifier);
        } catch (Exception e) {
            logger.warn("[{}] [{}] Failed",
                    context.partitionContext,
                    structureContext,
                    e);
        }
    }

    private void writeMotifs(Context context) {
        logger.info("[{}] Persisting {} unique word descriptors",
                context.partitionContext,
                context.buffer.size());

        final int bufferTotal = context.buffer.size();
        AtomicInteger bufferCount = new AtomicInteger();
        context.buffer.entrySet().parallelStream().forEach(entry -> {
            ResiduePairDescriptor full = entry.getKey();
            Map<String, List<ResiduePairIdentifier>> output = entry.getValue();

            if (bufferCount.incrementAndGet() % 100000 == 0) {
                logger.info("[{}] {} / {}",
                        context.partitionContext,
                        bufferCount,
                        bufferTotal);
            }

            motifLookup.insert(full, output);

            // writing takes additional heap - ease burden by dropping processed output bins
            output.clear();
        });
        context.buffer.clear();

        try {
            FileWriter processedWriter = new FileWriter(MotifSearch.INDEX_LIST.toFile(), true);
            for (String pdbId : context.processed) {
                processedWriter.append(pdbId).append("\n");
            }
            processedWriter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        context.processed.clear();
    }
}
