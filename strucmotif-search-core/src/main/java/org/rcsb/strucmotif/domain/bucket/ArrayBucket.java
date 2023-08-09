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
public class ArrayBucket implements Bucket {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty bucket which will refuse to iterate on structures or occurrences.
     */
    public static final ArrayBucket EMPTY_BUCKET = new ArrayBucket(EMPTY_INT_ARRAY, EMPTY_INT_ARRAY, EMPTY_INT_ARRAY) {
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
    public ArrayBucket(int[] structureIndices, int[] positionOffsets, int[] identifierData) {
        this.structureIndices = structureIndices;
        this.positionOffsets = positionOffsets;
        this.identifierData = identifierData;

        this.structurePointer = -1;
    }

    /**
     * Access to the structure index array.
     * @return an int[]
     */
    public int[] getStructureIndexArray() {
        return structureIndices;
    }

    /**
     * Access to the position offset array.
     * @return an int[]
     */
    public int[] getPositionOffsetArray() {
        return positionOffsets;
    }

    /**
     * Access to the actual data array.
     * @return an int[]
     */
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

    @Override
    public Set<Integer> getStructureIndices() {
        return Arrays.stream(structureIndices)
                .boxed()
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasNextStructure() {
        return structurePointer + 1 < positionOffsets.length;
    }

    @Override
    public boolean hasNextOccurrence() {
        return positionPointer + 2 < lastPosition;
    }

    @Override
    public void moveStructure() {
        structurePointer++;
        syncStructureState();
    }

    @Override
    public void moveOccurrence() {
        positionPointer += 2;
        if (positionPointer > lastPosition) {
            throw new IllegalStateException("Can't move to occurrence in another structure without calling moveStructure() first");
        }
    }

    @Override
    public int getStructureCount() {
        return structureIndices.length;
    }

    @Override
    public int getResiduePairCount() {
        return identifierData.length / 2;
    }

    @Override
    public int getStructureIndex() {
        return structureIndices[structurePointer];
    }

    @Override
    public long getResiduePairIdentifier() {
        return ResiduePairIdentifier.encodeIdentifier(identifierData[positionPointer], identifierData[positionPointer + 1]);
    }

    /**
     * Direct access into the identifier array by a given index.
     * @param pos array index
     * @return the identifier at that position
     */
    public int getResidueIndex(int pos) {
        return identifierData[pos];
    }

    @Override
    public void reset() {
        structurePointer = -1;
    }

    @Override
    public int getStartPosition() {
        return positionOffsets[structurePointer];
    }

    @Override
    public int getEndPosition() {
        return lastPosition;
    }
}
