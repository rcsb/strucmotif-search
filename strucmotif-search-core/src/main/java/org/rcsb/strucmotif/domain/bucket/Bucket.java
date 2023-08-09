package org.rcsb.strucmotif.domain.bucket;

import java.util.Set;

/**
 * Represents a single file of the inverted index (e.g., AL-4-5-4). This file keeps track of all structures that contain
 * occurrences of the corresponding descriptor.
 */
public interface Bucket {
    /**
     * All structures that contain occurrences of this descriptor.
     * @return a collection of structure indices
     */
    Set<Integer> getStructureIndices();

    /**
     * True if there is another structure after the current one.
     * @return a boolean
     */
    boolean hasNextStructure();

    /**
     * True if there is another occurrence after the current one.
     * @return a boolean
     */
    boolean hasNextOccurrence();

    /**
     * Advance to the next structure, only safe if {@link #hasNextStructure()} is true.
     */
    void moveStructure();

    /**
     * Advance to the next structure, only safe if {@link #hasNextOccurrence()} is true.
     */
    void moveOccurrence();

    /**
     * The number of structures that contain this descriptor.
     * @return an int
     */
    int getStructureCount();

    /**
     * The total number of residue pairs registered in this bucket.
     * @return an int
     */
    int getResiduePairCount();

    /**
     * The current structure index.
     * @return a structure index
     */
    int getStructureIndex();

    /**
     * Convenience method that gives access to the current identifier represented by 2 ints.
     * @return the current residue indices
     */
    long getResiduePairIdentifier();

    /**
     * Move iterators back to start positions.
     */
    void reset();

    /**
     * Get the current occurrence start position associated with the current position pointer.
     * @return an int
     */
    int getStartPosition();

    /**
     * Get the current occurrence end position associated with the current position pointer.
     * @return an int
     */
    int getEndPosition();
}
