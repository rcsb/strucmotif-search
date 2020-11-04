package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Revision;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

/**
 * The state of the application consists of two lists of {@link StructureIdentifier} instances:
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
    Collection<Pair<StructureIdentifier, Revision>> selectKnown();

    /**
     * The set of currently 'dirty' ids. Will be populated when update starts and emptied upon successful completion.
     * Problematic if not the empty set after an update.
     * @return a collection of ids
     */
    Collection<StructureIdentifier> selectDirty();

    /**
     * Insert into 'known' collection.
     * @param additions a collection of ids
     */
    void insertKnown(Collection<Pair<StructureIdentifier, Revision>> additions)

    /**
     * Insert into 'dirty' collection.
     * @param additions a collection of ids
     */
    void insertDirty(Collection<StructureIdentifier> additions);

    /**
     * Remove from 'known' collection.
     * @param removals a collection of ids
     */
    void deleteKnown(Collection<StructureIdentifier> removals);

    /**
     * Remove from 'dirty' collection.
     * @param removals a collection of ids
     */
    void deleteDirty(Collection<StructureIdentifier> removals);
}
