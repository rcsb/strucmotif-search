package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.bucket.ArrayBucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link InvertedIndex} that only deals with a single structure. Used in the
 * 'detect-motif' mode (see {@link org.rcsb.strucmotif.domain.query.MotifContextBuilder}).
 */
public class SingleStructureInvertedIndex implements InvertedIndex {
    private final Map<Integer, ArrayBucket> index;

    /**
     * Create an inverted index based on this graph.
     * @param residueGraph the content
     */
    public SingleStructureInvertedIndex(ResidueGraph residueGraph) {
         index = residueGraph.residuePairOccurrencesParallel()
                 .collect(Collectors.groupingBy(ResiduePairOccurrence::getResiduePairDescriptor, Collectors.collectingAndThen(Collectors.toList(), this::toInvertedIndexBucket)));
    }

    private ArrayBucket toInvertedIndexBucket(List<ResiduePairOccurrence> residuePairOccurrences) {
        int[] structureIndices = new int[] { 0 };
        int[] positionOffsets = new int[] { 0 };
        int[] positionData = new int[residuePairOccurrences.size() * 2];

        for (int i = 0; i < residuePairOccurrences.size(); i++) {
            ResiduePairOccurrence occurrence = residuePairOccurrences.get(i);
            positionData[2 * i] = occurrence.getResidueIndex1();
            positionData[2 * i + 1] = occurrence.getResidueIndex2();
        }

        return new ArrayBucket(structureIndices, positionOffsets, positionData);
    }

    @Override
    public void commit() {
        immutable();
    }

    @Override
    public ArrayBucket select(int residuePairDescriptor) {
        return index.getOrDefault(residuePairDescriptor, ArrayBucket.EMPTY_BUCKET);
    }

    @Override
    public void delete(Collection<Integer> structureIdentifiers) {
        immutable();
    }

    @Override
    public Set<Integer> reportKnownDescriptors() {
        return index.keySet();
    }

    @Override
    public Set<Integer> reportKnownKeys() {
        throw new UnsupportedOperationException("This bin contains a single not registered structure");
    }

    private void immutable() {
        throw new UnsupportedOperationException("This index is read-only");
    }
}
