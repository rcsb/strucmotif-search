package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.rcsb.strucmotif.persistence.StructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
public class DeleteStructuresFromStructureRepositoryTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromStructureRepositoryTask.class);
    private final StructureRepository structureRepository;
    private final StateRepository stateRepository;

    @Autowired
    public DeleteStructuresFromStructureRepositoryTask(StructureRepository structureRepository, StateRepository stateRepository) {
        this.structureRepository = structureRepository;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting removal of obsolete structures from structure repository");
        logger.info("{} structures to remove ({})",
                delta.size(),
                delta);

        if (delta.isEmpty()) {
            return;
        }

        for (StructureIdentifier id : delta) {
            structureRepository.delete(id);
            stateRepository.deleteSupported(Set.of(id));
        }

        logger.info("Finished cleanup of structure repository");
    }
}
