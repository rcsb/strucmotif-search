package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The specification on how to insert and select residue pair occurrences. Update operate is not directly supported
 * (rather invalid/obsolete identifiers have to be removed manually and subsequently the new data can be inserted).
 */
public interface InvertedIndex {
    /**
     * Insert operation for new data.
     * @param residuePairDescriptor the bin for which new data should be written
     * @param residuePairOccurrences the data to append to this bin - keys are pdbIds, values are all words of this descriptor
     * @param batchId extension of the temporary file to be written
     */
    void insert(ResiduePairDescriptor residuePairDescriptor, Bucket residuePairOccurrences, int batchId);

    /**
     * Perform lookup for a particular bin.
     * @param residuePairDescriptor the bin for which occurrences should the lookup be performed
     * @return a {@link Stream} of all occurrences, grouped by their structure identifier
     */
    InvertedIndexBucket select(ResiduePairDescriptor residuePairDescriptor);

    /**
     * Removes all information on a set of structures from the index.
     * @param structureIdentifiers what to remove
     */
    void delete(Collection<Integer> structureIdentifiers);

    /**
     * Scans the entire index and returns all referenced descriptors.
     * @return a collection of all descriptors
     */
    Set<ResiduePairDescriptor> reportKnownDescriptors();

    /**
     * Scans the entire index and returns all referenced structure indices.
     * @return a collection of all structure indices
     */
    Set<Integer> reportKnownKeys();
}
