package org.rcsb.strucmotif2.persistence;

import org.rcsb.strucmotif2.domain.StructureInformation;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The state of the application consists of two lists of {@link StructureIdentifier} instances:
 * <ul>
 *     <li><b>known:</b> entries that have been processed (might become invalid if e.g. alpha carbon trace)</li>
 *     <li><b>dirty:</b> all identifiers that could cause a corrupted state (if update fails during inverted index writing) - useful to recover</li>
 * </ul>
 *
 * This instance also does double duty and provides assembly information.
 */
public interface StateRepository {
    /**
     * Provides assembly information.
     * @param structureIdentifier the structure of interest
     * @return a map with operator expressions as key and a collection of all corresponding assembly ids as value
     */
    Map<String, Set<String>> selectAssemblyMap(StructureIdentifier structureIdentifier);

    /**
     * Returns all registered revisions.
     * @return a collection of ids and their corresponding revision
     */
    Collection<StructureInformation> selectKnown();

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
    void insertKnown(Collection<StructureInformation> additions);

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
