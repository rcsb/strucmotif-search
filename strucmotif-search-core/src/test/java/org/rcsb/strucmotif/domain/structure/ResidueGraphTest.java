package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;
import org.rcsb.strucmotif.math.Algebra;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.*;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.*;

class ResidueGraphTest {
    @Test
    public void whenGlycine_thenCreateVirtualBetaCarbon() {
        Map<LabelAtomId, float[]> residue = Map.of(LabelAtomId.N, new float[] { -0.966f, 0.493f, 1.500f },
                LabelAtomId.CA, new float[] { 0.257f, 0.418f, 0.692f },
                LabelAtomId.C, new float[] { -0.094f, 0.017f, -0.716f },
                LabelAtomId.O, new float[] { -1.056f, -0.682f, -0.923f });

        float[] betaCarbon = ResidueGraph.getVirtualCB(residue);
        assertArrayEquals(new float[]{1.204f, -0.620f, 1.296f}, betaCarbon, 0.01f);
    }

    private static final float TEST_DISTANCE_CUTOFF = 20;
    private StructureReader structureReader;
    private StrucmotifConfig strucmotifConfig;

    @BeforeEach
    public void init() {
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setDistanceCutoff(TEST_DISTANCE_CUTOFF);
        strucmotifConfig.setUndefinedAssemblies(false);
        structureReader = new StructureReaderImpl(new ResidueTypeResolverImpl(strucmotifConfig));
    }

    @Test
    public void whenResidueOrderSwapped_thenDescriptorSame() {
        List<String> perms = List.of("456", "465", "546", "564", "645", "654");
        Set<String> expectedDescriptors = Set.of("5-7-8", "4-5-7", "4-5-2");
        for (String perm : perms) {
            InputStream resource = getResource("cif/1acj-" + perm + ".cif");
            Structure structure = structureReader.readFromInputStream(resource);
            ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());
            Set<String> descriptors = residueGraph.residuePairOccurrencesSequential()
                    .map(ResiduePairOccurrence::getResiduePairDescriptor)
                    .peek(d -> assertFalse(d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
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
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        long c = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
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
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        List<String> o = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                // keep only freakish chain
                .filter(labelSelection -> labelSelection.getLabelAsymId().equals("C"))
                // map back to unique groups
                .map(labelSelection -> structure.getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId()))
                .map(index -> index + " " + structure.getResidueType(index))
                .collect(Collectors.toList());

        // sequence is T DVA P SAR MVA PXZ T DVA P SAR MVA
        // PXZ at pos 21 doesn't map to parent (also doesn't contain CA and/or CB)
        assertFalse(o.stream().anyMatch(l -> l.startsWith("21 ")));
        // SAR doesn't contain CB but parent component is GLY - so it'll use a virtual CB
        assertEquals(10, o.size());
    }

    @Test
    public void whenReadingOriginal200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertEquals(5949, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .peek(d -> assertFalse(d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .map(IndexSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingOriginalStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1acj"));
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertEquals(25187, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .peek(d -> assertFalse(d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .map(IndexSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingRenumbered200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("200l"));
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertEquals(5949,  residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .peek(d -> assertFalse(d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .map(IndexSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenReadingRenumberedStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1acj"));
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertEquals(25187, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .peek(d -> assertFalse(d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .map(IndexSelection::getStructOperId)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .map(indexSelection -> {
                    LabelSelection labelSelection = labelSelections.get(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                .map(LabelSelection::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    public void whenHomo8mer_thenPairCountIncreased() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        ResidueGraph residueGraphDeposited = new ResidueGraph(structure, strucmotifConfig, deposited());
        int pairingsDeposited = residueGraphDeposited.getNumberOfPairings();

        ResidueGraph residueGraphContacts = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());
        int pairingsContacts = residueGraphContacts.getNumberOfPairings();

        ResidueGraph residueGraphAssembly = new ResidueGraph(structure, strucmotifConfig, assembly("1"));
        int pairingsAssembly = residueGraphAssembly.getNumberOfPairings();

        ResidueGraph residueGraphAll = new ResidueGraph(structure, strucmotifConfig, all());
        int pairingsAll = residueGraphAll.getNumberOfPairings();

        assertTrue(pairingsDeposited < pairingsContacts, "including interface pairs must increase number");
        assertTrue(pairingsDeposited * 8 < pairingsAssembly, "contacts must be at least 8x (plus all interface contacts on top of that)");
        assertEquals(pairingsAssembly, pairingsAll, "count in assembly 1 must match all contact count");
    }

    private static final ResiduePairDescriptor ARGININE_TWEEZERS = new ResiduePairDescriptor(ResidueType.ARGININE,
            ResidueType.ARGININE,
            DistanceType.D15,
            DistanceType.D14,
            AngleType.A80);

    @Test
    public void whenArginineTweezers_thenReportMotifsInNonIdentityAssemblies() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("4ob8"));
        List<ResiduePairDescriptor> residuePairDescriptors = honorTolerance(ARGININE_TWEEZERS).collect(Collectors.toList());
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        List<ResiduePairIdentifier> identifiers = residueGraph.residuePairOccurrencesParallel()
                .filter(wordOccurrence -> residuePairDescriptors.contains(wordOccurrence.getResiduePairDescriptor()))
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .collect(Collectors.toList());
        assertFalse(identifiers.isEmpty());
        assertTrue(identifiers.stream()
                .flatMap(ResiduePairIdentifier::indexSelections)
                .map(IndexSelection::getStructOperId)
                .anyMatch(id -> !id.equals(Transformation.DEFAULT_OPERATOR)));
    }

    @Test
    public void whenReadingRNA_thenContactsFound() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7els"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertTrue(residueGraph.getNumberOfPairings() > 0);
    }

    @Test
    public void whenReadingStructureWithoutIdentityOper_thenContactsFound() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7a3x"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, depositedAndContacts());

        assertTrue(residueGraph.getNumberOfPairings() > 0);
    }

    @Test
    public void whenFlippedDirectory_thenAngle180() {
        // pointing away
        float angle = ResidueGraph.angle(new float[] { 1, 0, 0 }, new float[] { -1, 0, 0 });
        assertEquals(180, angle, 0.001);
    }

    @Test
    public void whenIdentical_thenAngle0() {
        // same
        float angle = ResidueGraph.angle(new float[] { 1, 0, 0 }, new float[] { 1, 0, 0 });
        assertEquals(0, angle, 0.001);
    }

    @Test
    public void whenOrthogonalInXy_thenAngle90() {
        // right 1
        float angle = ResidueGraph.angle(new float[] { 1, 0, 0 }, new float[] { 0, 1, 0 });
        assertEquals(90, angle, 0.001);
    }

    @Test
    public void whenOrthogonalInXz_thenAngle90() {
        // right 2
        float angle = ResidueGraph.angle(new float[] { 1, 0, 0 }, new float[] { 0, 0, 1 });
        assertEquals(90, angle, 0.001);
    }

    @Test
    public void whenOrderSwapped_thenAngleSame() {
        for (int i = 0; i < 10; i++) {
            float[] v1 = new float[] {(float) Math.random(), (float) Math.random(), (float) Math.random()};
            Algebra.normalize3d(v1, v1);
            float[] v2 = new float[] {(float) Math.random(), (float) Math.random(), (float) Math.random()};
            Algebra.normalize3d(v2, v2);
            float a1 = ResidueGraph.angle(v1, v2);
            float a2 = ResidueGraph.angle(v2, v1);
            assertEquals(a1, a2, 0.001f, "order of angle calculation must not matter");
        }
    }
}