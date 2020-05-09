package org.rcsb.strucmotif.core;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rcsb.strucmotif.GuiceJUnit4Runner;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.AuthorSelection;
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
        Structure structure = allPurposeReader.readById("4cha",
                List.of(new AuthorSelection("B", 1, 57), // H, B42
                        new AuthorSelection("B", 1, 102), // D, B87
                        new AuthorSelection("C", 1, 195))); // S, C47

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
                List.of(new AuthorSelection("B", 1, 14), // E, D11
                        new AuthorSelection("B", 1, 40), // Q, D37
                        new AuthorSelection("B", 1, 41), // I, D38
                        new AuthorSelection("B", 1, 54))); // R, D51

        MotifPruner motifPruner = new MotifPrunerImpl();
        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(structure);

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                // maximum alpha carbon distance is 7
                .allMatch(i -> i < 8));
    }
}