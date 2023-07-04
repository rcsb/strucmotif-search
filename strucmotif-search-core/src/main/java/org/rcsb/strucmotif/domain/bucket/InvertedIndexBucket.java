package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a single file of the inverted index (e.g., AL-4-5-4). This file keeps track of all structures that contain
 * occurrences of the corresponding descriptor.
 */
public class InvertedIndexBucket {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty bucket which will refuse to iterate on structures or occurrences.
     */
    public static final InvertedIndexBucket EMPTY_BUCKET = new InvertedIndexBucket(EMPTY_INT_ARRAY, EMPTY_INT_ARRAY, EMPTY_INT_ARRAY) {
        @Override
        public boolean hasNextStructure() {
            return false;
        }

        @Override
        public boolean hasNextOccurrence() {
            return false;
        }
    };

    // these are of equal length, equal to the number of referenced structures/entries
    private final int[] structureIndices; // just structure indices
    private final int[] positionOffsets; // points to the start index in the positionData array
    private final int[] identifierData; // length is equal to 2 * the number of residue pairs in this whole bin

    private int structurePointer; // the current position in the structureIndices/positionOffsets arrays

    private int positionPointer; // the current position in the positionData array
    private int lastPosition; // the last valid position in the positionData array that references the first position of a residue pair (after that the array will reference the next structure or end)

    /**
     * Construct an inverted index bucket from source array.
     * @param structureIndices structure identifiers
     * @param positionOffsets positional offsets, same length as structureIndices
     * @param identifierData identifiers data as encoded (int, int) tuples
     */
    public InvertedIndexBucket(int[] structureIndices, int[] positionOffsets, int[] identifierData) {
        this.structureIndices = structureIndices;
        this.positionOffsets = positionOffsets;
        this.identifierData = identifierData;

        this.structurePointer = -1;
    }

    public int[] getStructureIndexArray() {
        return structureIndices;
    }

    public int[] getPositionOffsetArray() {
        return positionOffsets;
    }

    public int[] getIdentifierDataArray() {
        return identifierData;
    }

    private void syncStructureState() {
        if (structurePointer >= positionOffsets.length) {
            throw new NoSuchElementException("No next structure");
        }
        this.positionPointer = positionOffsets[structurePointer] - 2;
        this.lastPosition = hasNextStructure() ? positionOffsets[structurePointer + 1] : identifierData.length;
    }

    /**
     * All structures that contain occurrences of this descriptor.
     * @return a collection of structure indices
     */
    public Set<Integer> getStructureIndices() {
        return Arrays.stream(structureIndices)
                .boxed()
                .collect(Collectors.toSet());
    }

    /**
     * True if there is another structure after the current one.
     * @return a boolean
     */
    public boolean hasNextStructure() {
        return structurePointer + 1 < positionOffsets.length;
    }

    /**
     * True if there is another occurrence after the current one.
     * @return a boolean
     */
    public boolean hasNextOccurrence() {
        return positionPointer + 2 < lastPosition;
    }

    /**
     * Advance to the next structure, only safe if {@link #hasNextStructure()} is true.
     */
    public void moveStructure() {
        structurePointer++;
        syncStructureState();
    }

    /**
     * Advance to the next structure, only safe if {@link #hasNextOccurrence()} is true.
     */
    public void moveOccurrence() {
        positionPointer += 2;
        if (positionPointer > lastPosition) {
            throw new IllegalStateException("Can't move to occurrence in another structure without calling moveStructure() first");
        }
    }

    /**
     * The number of structures that contain this descriptor.
     * @return an int
     */
    public int getStructureCount() {
        return structureIndices.length;
    }

    /**
     * The total number of residue pairs registered in this bucket.
     * @return an int
     */
    public int getResiduePairCount() {
        return identifierData.length / 2;
    }

    /**
     * The current structure index.
     * @return a structure index
     */
    public int getStructureIndex() {
        return structureIndices[structurePointer];
    }

    /**
     * Convenience method that gives access to the current identifier represented by a long.
     * @return the current residue indices
     */
    public long getResiduePairIdentifier() {
        return ResiduePairIdentifier.encodeIdentifier(identifierData[positionPointer], identifierData[positionPointer + 1]);
    }

    public int getResidueIndex1() {
        return identifierData[positionPointer];
    }

    public int getResidueIndex2() {
        return identifierData[positionPointer + 1];
    }

    public int getResidueIndex(int pos) {
        return identifierData[pos];
    }

    /**
     * Move iterators back to start positions.
     */
    public void reset() {
        structurePointer = -1;
    }

    public int getStartPosition() {
        return positionOffsets[structurePointer];
    }

    public int getEndPosition() {
        return lastPosition;
    }
}
