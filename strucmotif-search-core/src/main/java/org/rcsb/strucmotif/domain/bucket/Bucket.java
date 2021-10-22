package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.IndexSelectionResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.IndexSelection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single file of the inverted index (e.g., AL-4-5-4). This file keeps track of all structures that contain
 * occurrences of the corresponding descriptor.
 */
public interface Bucket {
    String DEFAULT_OPERATOR = "1";

    Set<Integer> getStructureIndices();

    boolean hasNextStructure();

    boolean hasNextOccurrence();

    void moveStructure();

    void moveOccurrence();

    int getStructureCount();

    int getResiduePairCount();

    int getStructureIndex();

    int getIndex1();

    int getIndex2();

    String getStructOperId1();

    String getStructOperId2();

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

                ResiduePairIdentifier residuePairIdentifier;
                // if short-cut to pair-identifiers is available: use it
                if (bucket instanceof ResiduePairIdentifierBucket) {
                    residuePairIdentifier = ((ResiduePairIdentifierBucket) bucket).getResiduePairIdentifier();
                } else {
                    IndexSelection indexSelection1 = new IndexSelection(bucket.getStructOperId1(), bucket.getIndex1());
                    IndexSelection indexSelection2 = new IndexSelection(bucket.getStructOperId2(), bucket.getIndex2());
                    residuePairIdentifier = new IndexSelectionResiduePairIdentifier(indexSelection1, indexSelection2);
                }
                identifiers.add(residuePairIdentifier);
            }
        }
    }

    static ResiduePairIdentifierBucket removeByKey(InvertedIndexBucket bucket, Collection<Integer> removals) {
        Map<Integer, Collection<ResiduePairIdentifier>> map = new HashMap<>();
        addAll(map, bucket, true, removals);
        return new ResiduePairIdentifierBucket(map);
    }

    InvertedIndexBucket EMPTY_BUCKET = new InvertedIndexBucket(null, null, null, null, null) {
        @Override
        public Set<Integer> getStructureIndices() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasNextStructure() {
            return false;
        }

        @Override
        public boolean hasNextOccurrence() {
            return false;
        }

        @Override
        public void moveStructure() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void moveOccurrence() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStructureCount() {
            return 0;
        }

        @Override
        public int getResiduePairCount() {
            return 0;
        }

        @Override
        public int getStructureIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIndex1() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIndex2() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getStructOperId1() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getStructOperId2() {
            throw new UnsupportedOperationException();
        }
    };
}
