package org.rcsb.strucmotif.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.selection.LabelSelectionResolver;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.read.StructureReaderImpl;
import org.rcsb.strucmotif.math.Algebra;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.*;

public class ResidueGraphTest {
    private static final double TEST_DISTANCE_CUTOFF = 20;
    private StructureReader structureReader;
    private MotifSearchConfig motifSearchConfig;

    @BeforeEach
    public void init() {
        motifSearchConfig = new MotifSearchConfig();
        motifSearchConfig.setDistanceCutoff(TEST_DISTANCE_CUTOFF);
        structureReader = new StructureReaderImpl();
    }

    @Test
    public void whenResidueOrderSwapped_thenDescriptorSame() {
        List<String> perms = List.of("456", "465", "546", "564", "645", "654");
        Set<String> expectedDescriptors = Set.of("5-7-8", "4-5-7", "4-5-2");
        for (String perm : perms) {
            InputStream resource = getResource("cif/1acj-" + perm + ".cif");
            Structure structure = structureReader.readFromInputStream(resource);
            ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());
            Set<String> descriptors = residueGraph.residuePairOccurrencesSequential()
                    .map(ResiduePairOccurrence::getResiduePairDescriptor)
                    .map(desc -> desc.getBackboneDistance().ordinal() + "-" + desc.getSideChainDistance().ordinal() + "-" + desc.getAngle().ordinal())
                    .collect(Collectors.toSet());
            for (String desc : descriptors) {
                assertTrue(expectedDescriptors.contains(desc), "descriptor definition not symmetric");
            }
        }
    }

    @Test
    public void whenReadingRenumbered3vvk_then6Valid() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3vvk"));
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        long c = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .distinct()
                .map(LabelSelection::getLabelAsymId)
                .distinct()
                // .peek(System.out::println)
                .count();

        // this is a structure with distinct chains in altlocs
        // currently, there is no support for altlocs (especially of this nature) - thus, many weird words will be reported - that's okay for now
        assertEquals(6, c);
    }

    @Test
    public void whenReadingRenumbered1dsd_then8ValidInChainC() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1dsd"));
        LabelSelectionResolver labelSelectionResolver = new LabelSelectionResolver(structure);
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        long c = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .distinct()
                // keep only freakish chain
                .filter(labelSelection -> labelSelection.getLabelAsymId().equals("C"))
                // map back to unique groups
                .map(labelSelectionResolver::resolve)
                .map(Residue::getResidueIdentifier)
                .map(componentIdentifier -> componentIdentifier + " " + componentIdentifier.getResidueType())
                .count();

        // sequence is T DVA P SAR MVA PXZ T DVA P SAR MVA
        // SAR and PXZ don't contain CA and/or CB - only 8 valid
        assertEquals(8, c);
    }

    @Test
    public void whenReadingOriginal200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        assertEquals(5939, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingOriginalStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1acj"));
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        assertEquals(25230, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingRenumbered200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        assertEquals(5947,  residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingRenumberedStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1acj"));
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        assertEquals(25196, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    private static final ResiduePairDescriptor ARGININE_TWEEZERS = new ResiduePairDescriptor(ResidueType.ARGININE,
            ResidueType.ARGININE,
            DistanceType.D15,
            DistanceType.D14,
            AngleType.A80,
            null);

    @Test
    public void whenArginineTweezers_thenReportMotifsInNonIdentityAssemblies() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("4ob8"));
        List<ResiduePairDescriptor> residuePairDescriptors = honorTolerance(ARGININE_TWEEZERS).collect(Collectors.toList());
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff());

        List<ResiduePairIdentifier> identifiers = residueGraph.residuePairOccurrencesParallel()
                .filter(wordOccurrence -> residuePairDescriptors.contains(wordOccurrence.getResiduePairDescriptor()))
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .collect(Collectors.toList());
        assertFalse(identifiers.isEmpty());
        assertTrue(identifiers.stream()
                .flatMap(ResiduePairIdentifier::labelSelections)
                .map(LabelSelection::getStructOperId)
                .anyMatch(id -> !id.equals("1")));
    }

    @Test
    public void whenFlippedDirectory_thenAngle180() {
        // pointing away
        double angle = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double [] { -1, 0, 0 });
        assertEquals(180, angle, 0.001);
    }

    @Test
    public void whenIdentical_thenAngle0() {
        // same
        double angle = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 1, 0, 0 });
        assertEquals(0, angle, 0.001);
    }

    @Test
    public void whenOrthogonalInXy_thenAngle90() {
        // right 1
        double angle = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 0, 1, 0 });
        assertEquals(90, angle, 0.001);
    }

    @Test
    public void whenOrthogonalInXz_thenAngle90() {
        // right 2
        double angle = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 0, 0, 1 });
        assertEquals(90, angle, 0.001);
    }

    @Test
    public void whenOrderSwapped_thenAngleSame() {
        for (int i = 0; i < 10; i++) {
            double[] v1 = Algebra.normalize3d(new double[] { Math.random(), Math.random(), Math.random() });
            double[] v2 = Algebra.normalize3d(new double[] { Math.random(), Math.random(), Math.random() });
            double a1 = ResidueGraph.angle(v1, v2);
            double a2 = ResidueGraph.angle(v2, v1);
            assertEquals(a1, a2, 0.001, "order of angle calculation must not matter");
        }
    }
}
