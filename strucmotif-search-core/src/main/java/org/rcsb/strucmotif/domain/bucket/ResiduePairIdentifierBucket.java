package org.rcsb.strucmotif.domain.bucket;

import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents a bucket that was created by the update process.
 */
public class ResiduePairIdentifierBucket implements Bucket {
    private final Map<Integer, Collection<ResiduePairIdentifier>> data;
    private Iterator<Map.Entry<Integer, Collection<ResiduePairIdentifier>>> structureIterator;
    private final int residuePairCount;
    private Map.Entry<Integer, Collection<ResiduePairIdentifier>> currentStructure;
    private Iterator<ResiduePairIdentifier> identifierIterator;
    private ResiduePairIdentifier currentIdentifier;

    /**
     * Delegate to the underlying map while implementing the bucket contract.
     * @param data actual data
     */
    public ResiduePairIdentifierBucket(Map<Integer, Collection<ResiduePairIdentifier>> data) {
        this.data = data;
        this.structureIterator = data.entrySet().iterator();
        this.residuePairCount = data.values().stream().mapToInt(Collection::size).sum();
    }

    @Override
    public Set<Integer> getStructureIndices() {
        return data.keySet();
    }

    @Override
    public boolean hasNextStructure() {
        return structureIterator.hasNext();
    }

    @Override
    public boolean hasNextOccurrence() {
        return identifierIterator.hasNext();
    }

    @Override
    public void moveStructure() {
        currentStructure = structureIterator.next();
        identifierIterator = currentStructure.getValue().iterator();
    }

    @Override
    public void moveOccurrence() {
        currentIdentifier = identifierIterator.next();
    }

    @Override
    public int getStructureCount() {
        return data.size();
    }

    @Override
    public int getResiduePairCount() {
        return residuePairCount;
    }

    @Override
    public int getStructureIndex() {
        return currentStructure.getKey();
    }

    @Override
    public int getIndex1() {
        return currentIdentifier.getIndex1();
    }

    @Override
    public int getIndex2() {
        return currentIdentifier.getIndex2();
    }

    @Override
    public String getStructOperId1() {
        return currentIdentifier.getStructOperId1();
    }

    @Override
    public String getStructOperId2() {
        return currentIdentifier.getStructOperId2();
    }

    @Override
    public ResiduePairIdentifier getResiduePairIdentifier() {
        return currentIdentifier;
    }

    @Override
    public void reset() {
        this.structureIterator = data.entrySet().iterator();
    }
}
