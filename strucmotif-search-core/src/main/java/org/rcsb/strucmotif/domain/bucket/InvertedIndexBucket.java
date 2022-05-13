package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.InvertedIndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gives access to the decoded data of an inverted index bucket.
 */
public class InvertedIndexBucket implements Bucket {
    private static final Map<Integer, String> EMPTY_MAP = Collections.emptyMap();
    private static final int[] EMPTY_ARRAY = new int[0];
    /**
     * An empty bucket which will refuse to iterate on structures or occurrences.
     */
    public static final InvertedIndexBucket EMPTY_BUCKET = new InvertedIndexBucket(EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY, EMPTY_ARRAY, new String[0]) {
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

    // length is equal to 2 * the number of residue pairs in this whole bin
    private final int[] positionData; // has the structure [occ1_index1, occ1_index2, occ2_index1, occ2_index2, occ3_index1, ...]

    private int structurePointer; // the current position in the structureIndices/positionOffsets arrays

    private int positionPointer; // the current position in the positionData array
    private int lastPosition; // the last valid position in the positionData array that references the first position of a residue pair (after that the array will reference the next structure or end)

    private final Map<Integer, String> operators;

    /**
     * Construct a inverted index bucket from source array.
     * @param structureIndices structure identifiers
     * @param positionOffsets positional offsets, same length as structureIndices
     * @param positionData positional data
     * @param operatorIndices sparse operator indices
     * @param operatorData sparse operator data, same length as operatorIndices
     */
    public InvertedIndexBucket(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
        this.structureIndices = structureIndices;
        this.positionOffsets = positionOffsets;
        this.positionData = positionData;
        if (operatorIndices.length > 0) {
            this.operators = new HashMap<>();
            for (int i = 0; i < operatorIndices.length; i++) {
                operators.put(operatorIndices[i], operatorData[i]);
            }
        } else {
            this.operators = EMPTY_MAP;
        }

        this.structurePointer = -1;
    }

    private void syncStructureState() {
        if (structurePointer >= positionOffsets.length) {
            throw new NoSuchElementException("No next structure");
        }
        this.positionPointer = positionOffsets[structurePointer] - 2;
        this.lastPosition = hasNextStructure() ? positionOffsets[structurePointer + 1] : positionData.length;
    }

    @Override
    public int getStructureCount() {
        return structureIndices.length;
    }

    @Override
    public int getResiduePairCount() {
        return positionData.length / 2;
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
    public void moveStructure() {
        structurePointer++;
        syncStructureState();
    }

    @Override
    public boolean hasNextOccurrence() {
        return positionPointer + 2 < lastPosition;
    }

    @Override
    public void moveOccurrence() {
        positionPointer += 2;
        if (positionPointer > lastPosition) {
            throw new IllegalStateException("Can't move to occurrence in another structure without calling moveStructure() first");
        }
    }

    /**
     * Get the current occurrence positions (for the current structure). This creates new array entirely, don't call if
     * not truly needed.
     * @return an int[]
     */
    public int[] getOccurrencePositions() {
        int start = positionOffsets[structurePointer];
        int end = lastPosition;
        int[] out = new int[(end - start) / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = start + i * 2;
        }
        return out;
    }

    @Override
    public int getStructureIndex() {
        return structureIndices[structurePointer];
    }

    /**
     * Direct access by index to the structure index.
     * @param i the position
     * @return an int
     */
    public int getStructureIndex(int i) {
        return structureIndices[i];
    }

    @Override
    public int getIndex1() {
        return positionData[positionPointer];
    }

    /**
     * Direct access by index to the index (in the atom[] of a structure).
     * @param i the position
     * @return an int
     */
    public int getIndex(int i) {
        return positionData[i];
    }

    @Override
    public int getIndex2() {
        return positionData[positionPointer + 1];
    }

    @Override
    public String getStructOperId1() {
        return operators.getOrDefault(positionPointer, Transformation.DEFAULT_OPERATOR);
    }

    /**
     * Direct access by index to struct_oper_id.
     * @param i the position
     * @return a String
     */
    public String getStructOperId(int i) {
        return operators.getOrDefault(i, Transformation.DEFAULT_OPERATOR);
    }

    @Override
    public String getStructOperId2() {
        return operators.getOrDefault(positionPointer + 1, Transformation.DEFAULT_OPERATOR);
    }

    @Override
    public ResiduePairIdentifier getResiduePairIdentifier() {
        return new InvertedIndexResiduePairIdentifier(getIndex1(), getIndex2(), getStructOperId1(), getStructOperId2());
    }

    @Override
    public void reset() {
        this.structurePointer = -1;
    }
}
