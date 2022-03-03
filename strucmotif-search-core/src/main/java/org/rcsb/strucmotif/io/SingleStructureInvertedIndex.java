package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.bucket.Bucket;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SingleStructureInvertedIndex implements InvertedIndex {
    private final Map<ResiduePairDescriptor, InvertedIndexBucket> index;

    public SingleStructureInvertedIndex(ResidueGraph residueGraph) {
         index = residueGraph.residuePairOccurrencesParallel()
                 .collect(Collectors.groupingBy(ResiduePairOccurrence::getResiduePairDescriptor, Collectors.collectingAndThen(Collectors.toList(), this::toBucket)));
    }

    @SuppressWarnings("Duplicates")
    private InvertedIndexBucket toBucket(List<ResiduePairOccurrence> residuePairOccurrences) {
        // TODO factor out all the bucket mapping logic
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
