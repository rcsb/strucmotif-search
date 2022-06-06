package org.rcsb.strucmotif.io.codec;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.bucket.Bucket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBucketCodec implements BucketCodec {
    @Override
    public ByteArrayOutputStream encode(Bucket bucket) throws IOException {
        ArrayBucket arrays = toArrays(bucket);
        return encode(arrays.getStructureIndices(), arrays.getPositionOffsets(), arrays.getPositionData(), arrays.getOperatorIndices(), arrays.getOperatorData());
    }

    /**
     * Encapsulate all arrays used by a bucket.
     */
    static class ArrayBucket {
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
     * @return a bunch of arrays without the methods of the Bucket interface
     */
    private static ArrayBucket toArrays(Bucket bucket) {
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
                if (!structOperId1.equals(Transformation.DEFAULT_OPERATOR)) {
                    operatorIndicesList.add(positionPointer);
                    operatorDataList.add(structOperId1);
                }
                if (!structOperId2.equals(Transformation.DEFAULT_OPERATOR)) {
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
}
