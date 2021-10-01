package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.motif.InvertedIndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.Collection;
import java.util.Map;
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
     */
    void insert(ResiduePairDescriptor residuePairDescriptor, Map<Integer, Collection<ResiduePairIdentifier>> residuePairOccurrences);

    /**
     * Perform lookup for a particular bin.
     * @param residuePairDescriptor the bin for which occurrences should the lookup be performed
     * @return a {@link Stream} of all occurrences, grouped by their structure identifier
     */
    Stream<Pair<Integer, InvertedIndexResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor);

    /**
     * Removes all information on a set of structures from the index.
     * @param structureIdentifiers what to remove
     */
    void delete(Collection<Integer> structureIdentifiers);
}
