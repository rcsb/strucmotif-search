package org.rcsb.strucmotif.domain.bucket;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class InvertedIndexBucket implements Bucket {
    // these are of equal length, equal to the number of referenced structures/entries
    private final int[] structureIndices; // just structure indices
    private final int[] positionOffsets; // points to the start index in the positionData array

    // length is equal to 2 * the number of residue pairs in this whole bin
    private final int[] positionData; // has the structure [occ1_index1, occ1_index2, occ2_index1, occ2_index2, occ3_index1, ...]

    // these are of equal length, might be 0, will only contain explicit values if operator is not equal to "1"
    private final int[] operatorIndices; // points to the index in the positionData array, each position with a non-default operator has an entry in this array, but definition in ascending order
    private final String[] operatorData; // just struct_oper_id strings

    private int structurePointer; // the current position in the structureIndices/positionOffsets arrays

    private int positionPointer; // the current position in the positionData array
    private int lastPosition; // the last valid position in the positionData array that references the first position of a residue pair (after that the array will reference the next structure or end)

    private int operatorPointer; // the current position in the operator array
    private int nextPositionWithOperator; // the next positionPointer that requires handling of struct_oper_id

    public InvertedIndexBucket(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
        this.structureIndices = structureIndices;
        this.positionOffsets = positionOffsets;
        this.positionData = positionData;
        this.operatorIndices = operatorIndices;
        this.operatorData = operatorData;

        this.structurePointer = -1;
        this.operatorPointer = 0;
        syncOperatorState();
    }

    private void syncStructureState() {
        this.positionPointer = positionOffsets[structurePointer];
        this.lastPosition = hasNextStructure() ? positionOffsets[structurePointer + 1] - 2 : positionOffsets.length - 2;
    }

    private void syncOperatorState() {
        this.nextPositionWithOperator = hasOperator() ? operatorIndices[operatorPointer] : -1;
    }

    private boolean hasOperator() {
        return operatorPointer < operatorIndices.length;
    }

    private boolean hasNextOperator() {
        return operatorPointer + 1 < operatorIndices.length;
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
        return positionPointer + 2 <= lastPosition;
    }

    @Override
    public void moveOccurrence() {
        // if this position has 1 or 2 operators then the operatorPointer must be advanced
        boolean operatorStateDirty = false;
        if (positionPointer == operatorPointer) {
            operatorPointer++;
            operatorStateDirty = true;
        }
        if (positionPointer == operatorPointer || positionPointer + 1 == operatorPointer) {
            operatorPointer++;
            operatorStateDirty = true;
        }
        if (operatorStateDirty) {
            syncOperatorState();
        }

        positionPointer += 2;
        if (positionPointer >= lastPosition) {
            throw new IllegalStateException("Can't move to occurrence in another structure without calling moveStructure() first");
        }
    }

    @Override
    public int getStructureIndex() {
        return structureIndices[structurePointer];
    }

    @Override
    public int getIndex1() {
        return positionData[positionPointer];
    }

    @Override
    public int getIndex2() {
        return positionData[positionPointer + 1];
    }

    @Override
    public String getStructOperId1() {
        if (positionPointer == nextPositionWithOperator) {
            return operatorData[operatorPointer];
        } else {
            return DEFAULT_OPERATOR;
        }
    }

    @Override
    public String getStructOperId2() {
        // the first index has an operator, this one might too
        if (positionPointer == nextPositionWithOperator && hasNextOperator() && positionPointer == operatorIndices[operatorPointer + 1]) {
            return operatorData[operatorPointer + 1];
        // the first index has no operator, the next operator occurs for the second index
        } else if (positionPointer + 1 == nextPositionWithOperator) {
            return operatorData[operatorPointer];
        } else {
            return DEFAULT_OPERATOR;
        }
    }
}
