package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.StructureInformation;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The state of the application consists of two lists of structure identifiers:
 * <ul>
 *     <li><b>known:</b> entries that have been processed (might become invalid if e.g. alpha carbon trace)</li>
 *     <li><b>dirty:</b> all identifiers that could cause a corrupted state (if update fails during inverted index writing) - useful to recover</li>
 * </ul>
 */
public interface StateRepository {
    /**
     * Returns all registered revisions.
     * @return a collection of ids and their corresponding revision
     */
    Set<StructureInformation> selectKnown();

    /**
     * Returns all referenced structure indices.
     * @return a collection of all structure indices
     */
    default Set<Integer> reportKnownKeys() {
        return selectKnown()
                .stream()
                .map(StructureInformation::structureIndex)
                .collect(Collectors.toSet());
    }

    /**
     * The set of currently 'dirty' ids. Will be populated when update starts and emptied upon successful completion.
     * Problematic if not the empty set after an update.
     * @return a collection of ids
     */
    Set<String> selectDirty();

    /**
     * Insert into 'known' collection.
     * @param additions a collection of ids
     */
    void insertKnown(Set<StructureInformation> additions);

    /**
     * Insert into 'dirty' collection.
     * @param additions a collection of ids
     */
    void insertDirty(Set<String> additions);

    /**
     * Remove from 'known' collection.
     * @param removals a collection of ids
     */
    void deleteKnown(Set<String> removals);

    /**
     * Remove from 'dirty' collection.
     * @param removals a collection of ids
     */
    void deleteDirty(Set<String> removals);
}
