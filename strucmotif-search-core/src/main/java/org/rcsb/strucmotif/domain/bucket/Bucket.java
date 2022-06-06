package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.ArrayList;
import java.util.List;
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
     * The current index1 value.
     * @return an int
     */
    int getIndex1();

    /**
     * The current index2 value.
     * @return an int
     */
    int getIndex2();

    /**
     * The current structOperId1 value.
     * @return a String
     */
    String getStructOperId1();

    /**
     * The current structOperId2 value.
     * @return a String
     */
    String getStructOperId2();

    /**
     * Convenience method that gives access to the current identifier represented by the individual index and
     * structOperId values.
     * @return the current {@link ResiduePairIdentifier}
     */
    ResiduePairIdentifier getResiduePairIdentifier();

    /**
     * Move iterators back to start positions.
     */
    void reset();
}
