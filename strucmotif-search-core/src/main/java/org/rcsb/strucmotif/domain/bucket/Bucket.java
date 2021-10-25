package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.InvertedIndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.ArrayList;
import java.util.Collection;
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
                    residuePairIdentifier = new InvertedIndexResiduePairIdentifier(bucket.getIndex1(), bucket.getIndex2(), bucket.getStructOperId1(), bucket.getStructOperId2());
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
}
