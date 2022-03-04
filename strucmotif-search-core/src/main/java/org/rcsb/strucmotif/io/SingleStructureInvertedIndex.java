package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link InvertedIndex} that only deals with a single structure. Used in the
 * 'detect-motif' mode (see {@link org.rcsb.strucmotif.domain.query.MotifContextBuilder}.
 */
public class SingleStructureInvertedIndex implements InvertedIndex {
    private final Map<ResiduePairDescriptor, InvertedIndexBucket> index;

    /**
     * Create an inverted index based on this graph.
     * @param residueGraph the content
     */
    public SingleStructureInvertedIndex(ResidueGraph residueGraph) {
         index = residueGraph.residuePairOccurrencesParallel()
                 .collect(Collectors.groupingBy(ResiduePairOccurrence::getResiduePairDescriptor, Collectors.collectingAndThen(Collectors.toList(), Bucket::toInvertedIndexBucket)));
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Bucket residuePairOccurrences) {
        immutable();
    }

    @Override
    public InvertedIndexBucket select(ResiduePairDescriptor residuePairDescriptor) {
        return index.getOrDefault(residuePairDescriptor, InvertedIndexBucket.EMPTY_BUCKET);
    }

    @Override
    public void delete(Collection<Integer> structureIdentifiers) {
        immutable();
    }

    @Override
    public Set<ResiduePairDescriptor> reportKnownDescriptors() {
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
