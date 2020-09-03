package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class MotifSearchUpdate {
    private static final String TASK_NAME = MotifSearchUpdate.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchUpdate.class);

    private final StateRepository stateRepository;
    private final AddStructuresToArchiveTask addStructuresToArchiveTask;
    private final AddStructuresToInvertedIndexTask addStructuresToInvertedIndexTask;
    private final AddStructuresToStructureRepositoryTask addStructuresToStructureRepositoryTask;
    private final DeleteStructuresFromArchiveTask deleteStructuresFromArchiveTask;
    private final DeleteStructuresFromInvertedIndexTask deleteStructuresFromInvertedIndexTask;
    private final DeleteStructuresFromStructureRepositoryTask deleteStructuresFromStructureRepositoryTask;

    @Autowired
    public MotifSearchUpdate(StateRepository stateRepository, AddStructuresToArchiveTask addStructuresToArchiveTask, AddStructuresToInvertedIndexTask addStructuresToInvertedIndexTask, AddStructuresToStructureRepositoryTask addStructuresToStructureRepositoryTask, DeleteStructuresFromArchiveTask deleteStructuresFromArchiveTask, DeleteStructuresFromInvertedIndexTask deleteStructuresFromInvertedIndexTask, DeleteStructuresFromStructureRepositoryTask deleteStructuresFromStructureRepositoryTask) {
        this.stateRepository = stateRepository;
        this.addStructuresToArchiveTask = addStructuresToArchiveTask;
        this.addStructuresToInvertedIndexTask = addStructuresToInvertedIndexTask;
        this.addStructuresToStructureRepositoryTask = addStructuresToStructureRepositoryTask;
        this.deleteStructuresFromArchiveTask = deleteStructuresFromArchiveTask;
        this.deleteStructuresFromInvertedIndexTask = deleteStructuresFromInvertedIndexTask;
        this.deleteStructuresFromStructureRepositoryTask = deleteStructuresFromStructureRepositoryTask;
    }

    public void handleAddOperation(Context context, Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = getKnownIdentifiers(context);
        Collection<StructureIdentifier> delta = getDeltaPlusIdentifiers(requested, known);
        switch (context) {
            case ARCHIVE:
                addStructuresToArchiveTask.execute(delta);
                break;
            case RESIDUE:
                addStructuresToStructureRepositoryTask.execute(delta);
                break;
            case INDEX:
                addStructuresToInvertedIndexTask.execute(delta);
                break;
        }
    }

    public void handleDeleteOperation(Context context, Collection<StructureIdentifier> requested) {
        Collection<StructureIdentifier> known = getKnownIdentifiers(context);
        Collection<StructureIdentifier> delta = getDeltaMinusIdentifiers(requested, known);
        switch (context) {
            case ARCHIVE:
                deleteStructuresFromArchiveTask.execute(delta);
                break;
            case RESIDUE:
                deleteStructuresFromStructureRepositoryTask.execute(delta);
                break;
            case INDEX:
                deleteStructuresFromInvertedIndexTask.execute(delta);
                break;
        }
    }

    /**
     * Determine all IDs that need to be added to the archive.
     * @param requested the requested update
     * @param known the registered identifiers
     * @return array of IDs that need to be processed for the given context
     */
    public Collection<StructureIdentifier> getDeltaPlusIdentifiers(Collection<StructureIdentifier> requested, Collection<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("[{}] No existing data - starting from scratch",
                    TASK_NAME);
            return requested;
        } else {
            return requested.stream()
                    .filter(id -> !known.contains(id))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Determine all IDs that need to be removed from the archive.
     * @param requested the requested update
     * @param known the registered identifiers
     * @return array of IDs that need to be remove for the given context
     */
    public Collection<StructureIdentifier> getDeltaMinusIdentifiers(Collection<StructureIdentifier> requested, Collection<StructureIdentifier> known) {
        if (known.isEmpty()) {
            logger.warn("[{}] No existing data - no need for cleanup of obsolete entries",
                    TASK_NAME);
            return Collections.emptySet();
        } else {
            return known.stream()
                    .filter(id -> !requested.contains(id))
                    .collect(Collectors.toSet());
        }
    }

    public Collection<StructureIdentifier> getKnownIdentifiers(Context context) {
        if (context == Context.ARCHIVE) {
            return stateRepository.selectKnown();
        } else if (context == Context.INDEX) {
            return stateRepository.selectIndexed();
        } else if (context == Context.RESIDUE) {
            return stateRepository.selectSupported();
        } else {
            throw new UnsupportedOperationException("Context " + context + " not supported");
        }
    }
}
