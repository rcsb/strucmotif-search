package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

/**
 * The state of the application consists of three lists of {@link StructureIdentifier} instances:
 * <ul>
 *     <li><b>known:</b> entries that have been processed (might become invalid if e.g. alpha carbon trace)</li>
 *     <li><b>indexed:</b> all entries registered in the inverted index</li>
 *     <li><b>supported:</b> all valid entries that encompass the search space</li>
 *     <li><b>dirty:</b> all identifiers that could cause a corrupted state (if update fails during inverted index writing) - useful to recover</li>
 * </ul>
 */
public interface StateRepository {
    /**
     * Returns all registered ids.
     * @return a collection of ids
     */
    Collection<StructureIdentifier> selectKnown();

    /**
     * Returns all ids that substitute the search space. Like 'known' but excluding ids that cannot be supported (e.g.
     * due to missing side-chain coordinates).
     * @return a collection of ids
     */
    Collection<StructureIdentifier> selectSupported();

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
    void insertKnown(Collection<StructureIdentifier> additions);

    /**
     * Insert into 'supported' collection.
     * @param additions a collection of ids
     */
    void insertSupported(Collection<StructureIdentifier> additions);

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
     * Remove from 'supported' collection.
     * @param removals a collection of ids
     */
    void deleteSupported(Collection<StructureIdentifier> removals);

    /**
     * Remove from 'dirty' collection.
     * @param removals a collection of ids
     */
    void deleteDirty(Collection<StructureIdentifier> removals);
}
