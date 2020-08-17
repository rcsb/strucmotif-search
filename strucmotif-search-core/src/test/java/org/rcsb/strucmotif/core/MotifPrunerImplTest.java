package org.rcsb.strucmotif.core;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rcsb.strucmotif.GuiceJUnit4Runner;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.AllPurposeReader;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(GuiceJUnit4Runner.class)
public class MotifPrunerImplTest {
    @Inject
    private AllPurposeReader allPurposeReader;

    @Test
    public void shouldPerformNoOperationForHDS() {
        Structure structure = Motifs.HDS.getStructure();

        MotifPruner motifPruner = new MotifPrunerImpl();
        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(structure);

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                .anyMatch(i -> i == 10));
    }

    @Test
    public void shouldPerformPruneForEQIR() {
        Structure structure = allPurposeReader.readById("1ec6",
                List.of(new LabelSelection("D", 1, 11), // E, D14
                        new LabelSelection("D", 1, 37), // Q, D40
                        new LabelSelection("D", 1, 38), // I, D41
                        new LabelSelection("D", 1, 51))); // R, D54

        MotifPruner motifPruner = new MotifPrunerImpl();
        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(structure);

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .peek(System.out::println)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                // maximum alpha carbon distance is 7
                .allMatch(i -> i < 8));
    }
}