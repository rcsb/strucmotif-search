package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

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
     * Merge two buckets. Doesn't allow duplicates.
     * @param bucket1 source1
     * @param bucket2 source2
     * @return a new bucket that contains the combined results of both
     */
    static ResiduePairIdentifierBucket merge(Bucket bucket1, Bucket bucket2) {
        Map<Integer, Collection<ResiduePairIdentifier>> map = new HashMap<>();
        addAll(map, bucket1, false, null);
        addAll(map, bucket2, true, null);
        return new ResiduePairIdentifierBucket(map);
    }

    private static void addAll(Map<Integer, Collection<ResiduePairIdentifier>> map, Bucket bucket, boolean noDuplicates, Collection<Integer> ignore) {
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            int key = bucket.getStructureIndex();
            if (ignore != null && ignore.contains(key)) {
                continue;
            }
            if (noDuplicates && map.containsKey(key)) {
                throw new IllegalStateException("Duplicate key: " + key);
            }
            Collection<ResiduePairIdentifier> identifiers = map.computeIfAbsent(key, e -> new ArrayList<>());

            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();

                ResiduePairIdentifier residuePairIdentifier = bucket.getResiduePairIdentifier();
                identifiers.add(residuePairIdentifier);
            }
        }
    }

    /**
     * Remove a collection of values from a bucket. Doesn't manipulate the original bucket.
     * @param bucket the source
     * @param removals what to remove
     * @return a new bucket that doesn't contain any of the removals
     */
    static ResiduePairIdentifierBucket removeByKey(InvertedIndexBucket bucket, Collection<Integer> removals) {
        Map<Integer, Collection<ResiduePairIdentifier>> map = new HashMap<>();
        addAll(map, bucket, true, removals);
        return new ResiduePairIdentifierBucket(map);
    }

    class BucketArrays {
        private final int[] structureIndices;
        private final int[] positionOffsets;
        private final int[] positionData;
        private final int[] operatorIndices;
        private final String[] operatorData;

        BucketArrays(int[] structureIndices, int[] positionOffsets, int[] positionData, int[] operatorIndices, String[] operatorData) {
            this.structureIndices = structureIndices;
            this.positionOffsets = positionOffsets;
            this.positionData = positionData;
            this.operatorIndices = operatorIndices;
            this.operatorData = operatorData;
        }

        public int[] getStructureIndices() {
            return structureIndices;
        }

        public int[] getPositionOffsets() {
            return positionOffsets;
        }

        public int[] getPositionData() {
            return positionData;
        }

        public int[] getOperatorIndices() {
            return operatorIndices;
        }

        public String[] getOperatorData() {
            return operatorData;
        }
    }

    static BucketArrays toArrays(Bucket bucket) {
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

        return new BucketArrays(structureIndices, positionOffsets, positionData, operatorIndices, operatorData);
    }
}
