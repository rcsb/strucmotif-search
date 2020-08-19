package org.rcsb.strucmotif.persistence;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The specification on how to insert and select residue pair occurrences. Update operate is not directly supported (rather
 * invalid/obsolete identifiers have to be removed manually and subsequently the new data can be inserted).
 */
public interface InvertedIndex {
    /**
     * Insert operation for new data.
     * @param residuePairDescriptor the bin for which new data should be written
     * @param residuePairOccurrences the data to append to this bin - keys are pdbIds, values are all words of this descriptor
     */
    void insert(ResiduePairDescriptor residuePairDescriptor, Map<String, List<ResiduePairIdentifier>> residuePairOccurrences);

    /**
     * Perform lookup for a particular bin.
     * @param residuePairDescriptor the bin for which occurrences should the lookup be performed
     * @return a {@link Stream} of all occurrences, grouped by {@link StructureIdentifier}
     */
    Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor);

    /**
     * Removes all information on a set of structures from a given file.
     * @param residuePairDescriptor the bin to manipulate
     * @param structureIdentifiers all structure identifiers to remove
     */
    void delete(ResiduePairDescriptor residuePairDescriptor, List<String> structureIdentifiers);

    /**
     * Create directories to store data when lookup table is created (it is not checked whether directories exist during
     * updating).
     * @throws IOException when creation fails
     */
    void createDirectories() throws IOException;
}
