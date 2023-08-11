package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.ResidueTypeResolver;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.DefaultStructureReader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

class KruskalMotifPrunerTest {
    private StrucmotifConfig strucmotifConfig;
    private StructureReader structureReader;
    private MotifPruner motifPruner;

    @BeforeEach
    public void init() {
        this.strucmotifConfig = new StrucmotifConfig();
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        this.structureReader = new DefaultStructureReader(residueTypeResolver);
        this.motifPruner = new KruskalMotifPruner();
    }

    @Test
    void whenHds_thenPerformNoOperation() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4cha"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("B", "1", 42), // H
                new LabelSelection("B", "1", 87), // D
                new LabelSelection("C", "1", 47)); // S
        List<Map<LabelAtomId, float[]>> residues = labelSelections.stream()
                .map(structure::getResidueIndex)
                .map(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(residues, labelSelections));

        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(residueGraph, Collections.emptyMap());

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                .anyMatch(i -> i == 10));
    }

    @Test
    void whenEqir_thenPruneOnePairing() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1ec6"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("D", "1", 11), // E, D14
                        new LabelSelection("D", "1", 37), // Q, D40
                        new LabelSelection("D", "1", 38), // I, D41
                        new LabelSelection("D", "1", 51)); // R, D54
        List<Map<LabelAtomId, float[]>> residues = labelSelections.stream()
                .map(structure::getResidueIndex)
                .map(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, ResidueGraph.ResidueGraphOptions.selection(residues, labelSelections));

        List<ResiduePairOccurrence> motifOccurrences = motifPruner.prune(residueGraph, Collections.emptyMap());

        assertEquals(3, motifOccurrences.size());
        assertTrue(motifOccurrences.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .map(ResiduePairDescriptor::getBackboneDistance)
                .mapToInt(DistanceType::getIntRepresentation)
                // maximum alpha carbon distance is 7
                .allMatch(i -> i < 8));
    }
}