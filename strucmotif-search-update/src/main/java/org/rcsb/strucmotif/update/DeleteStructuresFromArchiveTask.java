package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * The routine to remove files from the structural motif search tool (e.g. after superseding an ID).
 */
@Service
public class DeleteStructuresFromArchiveTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromArchiveTask.class);
    private final MotifSearchConfig motifSearchConfig;
    private final StateRepository stateRepository;

    @Autowired
    public DeleteStructuresFromArchiveTask(MotifSearchConfig motifSearchConfig, StateRepository stateRepository) {
        this.motifSearchConfig = motifSearchConfig;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting remove structures from archive");

        // for each id: remove reduced file and
        delta.forEach(id -> {
                    try {
                        Path path = motifSearchConfig.getArchivePath().resolve(id.getPdbId() + ".bcif.gz");
                        if (Files.exists(path)) {
                            logger.info("[{}] Removing optimized structure", id.getPdbId());
                            Files.delete(path);
                        }
                        stateRepository.deleteKnown(Set.of(id));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        logger.info("Finished archive cleanup");
    }
}
