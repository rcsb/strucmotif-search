package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single file of the inverted index (e.g., AL-4-5-4). This file keeps track of all structures that contain
 * occurrences of the corresponding descriptor.
 */
public interface Bucket {
    /**
     * The default operator if no assembly information is defined explicitly.
     */
    String DEFAULT_OPERATOR = "1";

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

    /**
     * Encapsulate all arrays used by a bucket.
     */
    class ArrayBucket {
        private final int[] structureIndices;
        private final int[] positionOffsets;
        private final int[] positionData;
        private final int[] operatorIndices;
        private final String[] operatorData;

        private ArrayBucket(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
            this.structureIndices = structureIndices;
            this.positionOffsets = positionOffsets;
            this.positionData = positionData;
            this.operatorIndices = operatorIndices;
            this.operatorData = operatorData;
        }

        /**
         * Access to the structure indices.
         * @return an int[]
         */
        public int[] getStructureIndices() {
            return structureIndices;
        }

        /**
         * Access to the position offsets.
         * @return an int[]
         */
        public int[] getPositionOffsets() {
            return positionOffsets;
        }

        /**
         * Access to the position data.
         * @return an int[]
         */
        public int[] getPositionData() {
            return positionData;
        }

        /**
         * Access to the operator indices.
         * @return an int[]
         */
        public int[] getOperatorIndices() {
            return operatorIndices;
        }

        /**
         * Access to the operator data.
         * @return a String[]
         */
        public String[] getOperatorData() {
            return operatorData;
        }
    }

    /**
     * Convert a bucket to a group of arrays.
     * @param bucket the source
     * @return a bunch of arrays without the methods of the Bucket interface
     */
    static ArrayBucket toArrayBucket(Bucket bucket) {
        int structureCount = bucket.getStructureCount();
        int[] structureIndices = new int[structureCount];
        int[] positionOffsets = new int[structureCount];
        int[] positionData = new int[bucket.getResiduePairCount() * 2];
        List<Integer> operatorIndicesList = new ArrayList<>();
        List<String> operatorDataList = new ArrayList<>();

        int structurePointer = 0;
        int positionPointer = 0;
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            structureIndices[structurePointer] = bucket.getStructureIndex();
            positionOffsets[structurePointer] = positionPointer;

            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();
                positionData[positionPointer] = bucket.getIndex1();
                positionData[positionPointer + 1] = bucket.getIndex2();

                String structOperId1 = bucket.getStructOperId1();
                String structOperId2 = bucket.getStructOperId2();
                if (!structOperId1.equals(Bucket.DEFAULT_OPERATOR)) {
                    operatorIndicesList.add(positionPointer);
                    operatorDataList.add(structOperId1);
                }
                if (!structOperId2.equals(Bucket.DEFAULT_OPERATOR)) {
                    operatorIndicesList.add(positionPointer + 1);
                    operatorDataList.add(structOperId2);
                }

                positionPointer += 2;
            }

            structurePointer++;
        }

        int[] operatorIndices = operatorIndicesList.stream().mapToInt(Integer::intValue).toArray();
        String[] operatorData = operatorDataList.toArray(String[]::new);

        return new ArrayBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
    }

    /**
     * Convert a collection of {@link ResiduePairOccurrence} instances to an {@link InvertedIndexBucket}.
     * @param residuePairOccurrences source data
     * @return an {@link InvertedIndexBucket}
     */
    static InvertedIndexBucket toInvertedIndexBucket(List<ResiduePairOccurrence> residuePairOccurrences) {
        int[] structureIndices = new int[] { 0 };
        int[] positionOffsets = new int[] { 0 };
        int[] positionData = new int[residuePairOccurrences.size() * 2];
        List<Integer> operatorIndicesList = new ArrayList<>();
        List<String> operatorDataList = new ArrayList<>();

        for (int i = 0; i < residuePairOccurrences.size(); i++) {
            ResiduePairIdentifier identifier = residuePairOccurrences.get(i).getResidueIdentifier();
            positionData[2 * i] = identifier.getIndex1();
            positionData[2 * i + 1] = identifier.getIndex2();

            String structOperId1 = identifier.getStructOperId1();
            String structOperId2 = identifier.getStructOperId2();
            if (!structOperId1.equals(Bucket.DEFAULT_OPERATOR)) {
                operatorIndicesList.add(2 * i);
                operatorDataList.add(structOperId1);
            }
            if (!structOperId2.equals(Bucket.DEFAULT_OPERATOR)) {
                operatorIndicesList.add(2 * i + 1);
                operatorDataList.add(structOperId2);
            }
        }

        int[] operatorIndices = operatorIndicesList.stream().mapToInt(Integer::intValue).toArray();
        String[] operatorData = operatorDataList.toArray(String[]::new);
        return new InvertedIndexBucket(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
    }
}
