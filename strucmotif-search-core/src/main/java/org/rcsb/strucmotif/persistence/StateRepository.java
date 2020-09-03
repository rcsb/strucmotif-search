package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

/**
 * The state of the application consists of three lists of {@link StructureIdentifier} instances:
 * <ul>
 *     <li><b>known:</b> entries that have been processed (might become invalid if e.g. alpha carbon trace)</li>
 *     <li><b>indexed:</b> all entries registered in the inverted index</li>
 *     <li><b>supported:</b> all valid entries that encompass the search space</li>
 * </ul>
 */
public interface StateRepository {
    Collection<StructureIdentifier> selectKnown();

    Collection<StructureIdentifier> selectSupported();

    Collection<StructureIdentifier> selectIndexed();

    void insertKnown(Collection<StructureIdentifier> additions);

    void insertSupported(Collection<StructureIdentifier> additions);

    void insertIndexed(Collection<StructureIdentifier> additions);

    void deleteKnown(Collection<StructureIdentifier> removals);

    void deleteSupported(Collection<StructureIdentifier> removals);

    void deleteIndexed(Collection<StructureIdentifier> removals);
}
