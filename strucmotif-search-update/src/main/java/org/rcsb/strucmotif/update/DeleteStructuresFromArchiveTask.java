package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.UpdateStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * The routine to remove files from the structural motif search tool (e.g. after superseding an ID).
 */
public class DeleteStructuresFromArchiveTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromArchiveTask.class);
    private static final String TASK_NAME = DeleteStructuresFromArchiveTask.class.getSimpleName();

    public DeleteStructuresFromArchiveTask(Set<StructureIdentifier> identifiers, UpdateStateManager updateStateManager) {
        logger.info("[{}] Starting remove structures from archive",
                TASK_NAME);

        // for each id: remove reduced file and
        identifiers.forEach(id -> {
                    try {
                        Path path = MotifSearch.ARCHIVE_PATH.resolve(id + ".bcif");
                        if (Files.exists(path)) {
                            logger.info("[{}] [{}] Removing optimized structure",
                                    id,
                                    TASK_NAME);
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        updateStateManager.deleteArchiveEntries(identifiers);

        logger.info("[{}] Finished archive cleaning",
                TASK_NAME);
    }
}
