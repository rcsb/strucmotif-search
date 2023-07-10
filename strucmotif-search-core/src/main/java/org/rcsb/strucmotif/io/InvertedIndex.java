package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The specification on how to insert and select residue pair occurrences. Update operate is not directly supported
 * (rather invalid/obsolete identifiers have to be removed manually and subsequently the new data can be inserted).
 * Insert operations are implemented by the update routine. It will place temporary files next to the index files and by
 * calling {@link #commit()}, these can be added to the index.
 */
public interface InvertedIndex extends Committable {
    /**
     * Perform lookup for a particular bin.
     * @param residuePairDescriptor the bin for which occurrences should the lookup be performed
     * @return a {@link Stream} of all occurrences, grouped by their structure identifier
     */
    InvertedIndexBucket select(int residuePairDescriptor);

    /**
     * Removes all information on a set of structures from the index.
     * @param structureIdentifiers what to remove
     */
    void delete(Collection<Integer> structureIdentifiers);

    /**
     * Scans the entire index and returns all referenced descriptors.
     * @return a collection of all descriptors
     */
    Set<Integer> reportKnownDescriptors();

    /**
     * Scans the entire index and returns all referenced structure indices.
     * @return a collection of all structure indices
     */
    Set<Integer> reportKnownKeys();
}
