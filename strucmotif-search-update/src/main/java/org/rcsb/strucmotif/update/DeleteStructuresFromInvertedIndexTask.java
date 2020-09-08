package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * Will cleanup the inverted index in a rather time-intensive manner.
 */
@Service
public class DeleteStructuresFromInvertedIndexTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromInvertedIndexTask.class);
    private final InvertedIndex invertedIndex;
    private final StateRepository stateRepository;

    @Autowired
    public DeleteStructuresFromInvertedIndexTask(InvertedIndex invertedIndex, StateRepository stateRepository) {
        this.invertedIndex = invertedIndex;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting removal of obsolete structures from inverted index");
        logger.info("{} structures to remove ({})",
                delta.size(),
                delta);

        if (delta.isEmpty()) {
            return;
        }

        for (StructureIdentifier structureIdentifier : delta) {
            invertedIndex.delete(structureIdentifier);
            stateRepository.deleteIndexed(Set.of(structureIdentifier));
        }

        logger.info("Finished cleanup of inverted index");
    }
}
