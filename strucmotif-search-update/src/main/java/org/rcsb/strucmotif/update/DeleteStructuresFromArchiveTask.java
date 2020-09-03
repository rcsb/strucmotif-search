package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.MongoStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * The routine to remove files from the structural motif search tool (e.g. after superseding an ID).
 */
@Service
public class DeleteStructuresFromArchiveTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromArchiveTask.class);
    private static final String TASK_NAME = DeleteStructuresFromArchiveTask.class.getSimpleName();
    private final MotifSearchConfig motifSearchConfig;
    private final MongoStateRepository stateRepository;

    @Autowired
    public DeleteStructuresFromArchiveTask(MotifSearchConfig motifSearchConfig, MongoStateRepository stateRepository) {
        this.motifSearchConfig = motifSearchConfig;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("[{}] Starting remove structures from archive",
                TASK_NAME);

        // for each id: remove reduced file and
        delta.forEach(id -> {
                    try {
                        Path path = motifSearchConfig.getArchivePath().resolve(id + ".bcif.gz");
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

        stateRepository.deleteKnown(delta);

        logger.info("[{}] Finished archive cleaning",
                TASK_NAME);
    }
}
