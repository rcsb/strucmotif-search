package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.MongoResidueDB;
import org.rcsb.strucmotif.persistence.MongoTitleDB;
import org.rcsb.strucmotif.persistence.UpdateStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class RemoveStructuresFromStructureDBTask {
    private static final Logger logger = LoggerFactory.getLogger(RemoveStructuresFromStructureDBTask.class);
    private static final String TASK_NAME = RemoveStructuresFromStructureDBTask.class.getSimpleName();

    public RemoveStructuresFromStructureDBTask(Set<StructureIdentifier> identifiers, MongoResidueDB residueDB, MongoTitleDB titleDB, UpdateStateManager updateStateManager) throws IOException {
        logger.info("[{}] Starting removal of obsolete structures from component-DB",
                TASK_NAME);

        logger.info("[{}] {} structures to remove ({})",
                TASK_NAME,
                identifiers.size(),
                identifiers);

        if (identifiers.isEmpty()) {
            return;
        }

        for (StructureIdentifier id : identifiers) {
            residueDB.deleteResidues(id);
            titleDB.deleteTitle(id);
        }

        updateStateManager.deleteResidueDBEntries(identifiers);

        logger.info("[{}] Finished cleanup of residue-DB",
                TASK_NAME);
    }
}
