package org.rcsb.strucmotif.update;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.UpdateStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates dedicated, reduced representation of all structure files. Prepares structures to be added to the index.
 */
public class AddStructuresToArchiveTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToArchiveTask.class);
    private static final String TASK_NAME = AddStructuresToArchiveTask.class.getSimpleName();

    public AddStructuresToArchiveTask(Set<StructureIdentifier> identifiers, StructureWriter renumberedWriter, UpdateStateManager updateStateManager) {
        logger.info("[{}] Starting structural motif search archive update",
                TASK_NAME);

        AtomicInteger counter = new AtomicInteger();
        int target = identifiers.size();

        // write structure
        identifiers.parallelStream()
                .forEach(id -> {
                    try {
                        logger.info("[{} / {}] Renumbering {}",
                                counter.incrementAndGet(),
                                target,
                                id);

                        // ensure directories exist
                        Files.createDirectories(MotifSearch.ARCHIVE_PATH);

                        MmCifFile cifFile = readById(id.getPdbId()).as(StandardSchemata.MMCIF);
                        renumberedWriter.write(cifFile);
                    } catch (IOException e) {
                        logger.warn("[{} / {}] {} failed - no source file @ RCSB",
                                counter.get(),
                                target,
                                id, e);
                    }
                });

        updateStateManager.insertArchiveEntries(identifiers);

        logger.info("[{}] Finished archive update",
                TASK_NAME);
    }

    protected CifFile readById(String pdbId) throws IOException {
        return CifIO.readById(pdbId);
    }
}
