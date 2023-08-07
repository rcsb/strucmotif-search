package org.rcsb.strucmotif.domain.motif;

import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResiduePairOccurrenceTest {
    @Test
    void whenStreamingDescriptorsWithExchanges_thenAllPresent() {
        // DD-5-4-4 -> 10-55
        ResiduePairOccurrence occurrence = new ResiduePairOccurrence(42949673015L, 8522308);
        Map<Integer, Set<ResidueType>> exchanges = Map.of(10, Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID));

        List<Integer> out = occurrence.residuePairDescriptorsByTolerance(1, 1, 1, exchanges)
                .boxed()
                .toList();

        assertEquals(out.stream().distinct().count(), out.size(), "There are duplicates");
        assertEquals(2, out.stream().map(ResiduePairDescriptor::getResidueType2).distinct().count(), "Didn't observe exchange");
    }
}