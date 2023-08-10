package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.io.DefaultStructureReader;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.math.Algebra;

import java.io.InputStream;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.*;
import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.*;

class ResidueGraphTest {
    @Test
    void whenGlycine_thenCreateVirtualBetaCarbon() {
        Map<LabelAtomId, float[]> residue = Map.of(LabelAtomId.N, new float[] { -0.966f, 0.493f, 1.500f },
                LabelAtomId.CA, new float[] { 0.257f, 0.418f, 0.692f },
                LabelAtomId.C, new float[] { -0.094f, 0.017f, -0.716f },
                LabelAtomId.O, new float[] { -1.056f, -0.682f, -0.923f });

        float[] betaCarbon = ResidueGraph.getVirtualCB(residue);
        assertArrayEquals(new float[] { 1.204f, -0.620f, 1.296f }, betaCarbon, 0.01f);
    }

    private static final float TEST_DISTANCE_CUTOFF = 20;
    private DefaultStructureReader structureReader;
    private StrucmotifConfig strucmotifConfig;

    @BeforeEach
    public void init() {
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setDistanceCutoff(TEST_DISTANCE_CUTOFF);
        structureReader = new DefaultStructureReader(new DefaultResidueTypeResolver(strucmotifConfig));
    }

    @Test
    void whenResidueOrderSwapped_thenDescriptorSame() {
        List<String> perms = List.of("456", "465", "546", "564", "645", "654");
        Set<String> expectedDescriptors = Set.of("5-7-8", "4-5-7", "4-5-3");
        for (String perm : perms) {
            InputStream resource = getResource("cif/1acj-" + perm + ".cif");
            Structure structure = structureReader.readFromInputStream(resource);
            ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());
            Set<String> descriptors = residueGraph.residuePairOccurrencesSequential()
                    .peek(o -> assertFalse(o.getBackboneDistance() == DistanceType.D0 || o.getSideChainDistance() == DistanceType.D0, "Has zero-distance contacts"))
                    .map(o -> o.getBackboneDistance().ordinal() + "-" + o.getSideChainDistance().ordinal() + "-" + o.getAngle().ordinal())
                    .collect(Collectors.toSet());
            for (String desc : descriptors) {
                assertTrue(expectedDescriptors.contains(desc), "descriptor definition not symmetric, '" + desc + "' not registered in set of expected descriptors");
            }
        }
    }

    @Test
    void whenReadingRenumbered3vvk_then6Valid() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3vvk"));
        assertEquals(1556, structure.getModelledResidueCount());
        assertEquals(1556, structure.getInstancedResidueCount());
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());
        assertEquals(1556, residueGraph.getResidueCount());

        long c = residueGraph.residuePairOccurrencesSequential()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getLabelAsymId)
                .distinct()
                .count();

        // this is a structure with distinct chains in altlocs
        // currently, there is no support for altlocs (especially of this nature) - thus, many weird words will be reported - that's okay for now
        assertEquals(6, c, "Less than the expected number of chains, only saw: " + residueGraph.residuePairOccurrencesSequential().flatMapToInt(ResiduePairOccurrence::residueIndices).mapToObj(structure::getLabelAsymId).distinct().toList());
    }

    @Test
    void whenReadingRenumbered1dsd_then8ValidInChainC() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1dsd"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        List<String> o = residueGraph.residuePairOccurrencesSequential()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .distinct()
                .sorted()
                // keep only freakish chain
                .filter(residueIndex -> structure.getLabelAsymId(residueIndex).equals("C"))
                .mapToObj(index -> structure.getLabelSeqId(index) + " " + structure.getResidueType(index))
                .toList();
        IntSummaryStatistics stats = o.stream().map(i -> i.split(" ")[0]).mapToInt(Integer::parseInt).summaryStatistics();

        assertEquals(1, stats.getMin(), "First residue must be at position 1");
        assertEquals(11, stats.getMax(), "Last residue must be at position 11");
        // sequence is T DVA P SAR MVA PXZ T DVA P SAR MVA
        // PXZ at pos 21 doesn't map to parent (also doesn't contain CA and/or CB)
        assertFalse(o.stream().anyMatch(l -> l.startsWith("21 ")));
        // SAR doesn't contain CB but parent component is GLY - so it'll use a virtual CB
        assertEquals(10, o.size());
    }

    @Test
    void whenReadingOriginal200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.residuePairOccurrencesSequential().noneMatch(d -> d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0), "Has zero-distance contacts");
        assertEquals(6393, residueGraph.residuePairOccurrencesParallel()
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getTransformationIdentifier)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    void whenReadingOriginalStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1acj"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.residuePairOccurrencesSequential().noneMatch(d -> d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0), "Has zero-distance contacts");
        assertEquals(64246, residueGraph.residuePairOccurrencesParallel()
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getTransformationIdentifier)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .map(structure::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    void whenReadingRenumbered200l_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("200l"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.residuePairOccurrencesSequential().noneMatch(d -> d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0), "Has zero-distance contacts");
        assertEquals(6393,  residueGraph.residuePairOccurrencesParallel()
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getTransformationIdentifier)
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    void whenReadingRenumberedStructureWithAssemblies_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1acj"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.residuePairOccurrencesSequential().noneMatch(d -> d.getBackboneDistance() == DistanceType.D0 || d.getSideChainDistance() == DistanceType.D0), "Has zero-distance contacts");
        assertEquals(64246, residueGraph.residuePairOccurrencesParallel()
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getTransformationIdentifier)
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .mapToObj(structure::getLabelSeqId)
                .distinct()
                .count());
    }

    @Test
    void whenHomo8mer_thenPairCountIncreased() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        ResidueGraph residueGraphDeposited = new ResidueGraph(structure, strucmotifConfig, deposited());
        int pairingsDeposited = residueGraphDeposited.getPairingCount();

        ResidueGraph residueGraphContacts = new ResidueGraph(structure, strucmotifConfig, chainsInContact());
        int pairingsContacts = residueGraphContacts.getPairingCount();

        ResidueGraph residueGraphAssembly = new ResidueGraph(structure, strucmotifConfig, assembly("1"));
        int pairingsAssembly = residueGraphAssembly.getPairingCount();

        List<LabelSelection> labelSelections = structure.instancedResidueIndices()
                .mapToObj(i -> new LabelSelection(structure.getLabelAsymId(i), structure.getTransformationIdentifier(i), structure.getLabelSeqId(i)))
                .toList();
        List<Map<LabelAtomId, float[]>> residues = structure.instancedResidueIndices()
                .mapToObj(structure::manifestResidue)
                .toList();
        ResidueGraph residueGraphAll = new ResidueGraph(structure, strucmotifConfig, selection(residues, labelSelections));
        int pairingsAll = residueGraphAll.getPairingCount();

        assertTrue(pairingsDeposited < pairingsContacts, "including interface pairs must increase number");
        assertTrue(pairingsDeposited * 8 < pairingsAssembly, "contacts must be at least 8x (plus all interface contacts on top of that)");
        assertEquals(pairingsAssembly, pairingsAll, "count in assembly 1 must match all contact count");
    }

    private static final int DESCRIPTOR = ResiduePairDescriptor.encodeDescriptor(ResidueType.ARGININE, ResidueType.ARGININE, DistanceType.D15, DistanceType.D14, AngleType.A80);
    private static final ResiduePairOccurrence OCCURRENCE = new ResiduePairOccurrence(0L, DESCRIPTOR);

    @Test
    void whenArginineTweezers_thenReportMotifsInNonIdentityAssemblies() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("4ob8"));
        Set<Integer> residuePairDescriptors = OCCURRENCE.residuePairDescriptorsByTolerance(1, 1, 1, Collections.emptyMap()).boxed().collect(Collectors.toSet());
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        List<Integer> identifiers = residueGraph.residuePairOccurrencesParallel()
                .filter(wordOccurrence -> residuePairDescriptors.contains(wordOccurrence.getResiduePairDescriptor()))
                .flatMapToInt(ResiduePairOccurrence::residueIndices)
                .boxed()
                .toList();
        assertFalse(identifiers.isEmpty());
        assertTrue(identifiers.stream().anyMatch(id -> !structure.getTransformationIdentifier(id).endsWith("_1")));
    }

    @Test
    void whenReadingRNA_thenContactsFound() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7els"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.getPairingCount() > 0);
    }

    @Test
    void whenReadingStructureWithoutIdentityOper_thenContactsFound() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7a3x"));
        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, chainsInContact());

        assertTrue(residueGraph.getPairingCount() > 0);
    }

    @Test
    void whenAssembliesInTheSamePlace_thenNoIndexingZeroDistancePairs() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("3djy"));

        ResidueGraph deposited = new ResidueGraph(structure, strucmotifConfig, deposited());
        assertNoZeroDistancePairs(structure, deposited);

        ResidueGraph residuesInContact = new ResidueGraph(structure, strucmotifConfig, residuesInContact());
        assertNoZeroDistancePairs(structure, residuesInContact);

        ResidueGraph chainsInContact = new ResidueGraph(structure, strucmotifConfig, chainsInContact());
        assertNoZeroDistancePairs(structure, chainsInContact);
    }

    private void assertNoZeroDistancePairs(Structure structure, ResidueGraph residueGraph) {
        boolean fail = residueGraph.residuePairOccurrencesSequential()
                .anyMatch(o -> ResiduePairDescriptor.getBackboneDistance(o.getResiduePairDescriptor()) == DistanceType.D0);

        if (fail) {
            System.out.println("Offenders:");
            residueGraph.residuePairOccurrencesSequential()
                    .filter(o -> ResiduePairDescriptor.getBackboneDistance(o.getResiduePairDescriptor()) == DistanceType.D0)
                    .map(o -> {
                        int r1 = ResiduePairIdentifier.getResidueIndex1(o.getResiduePairIdentifier());
                        int r2 = ResiduePairIdentifier.getResidueIndex2(o.getResiduePairIdentifier());
                        return structure.getAssemblyIdentifier(r1) + "@" + structure.getLabelSelection(r1) + " " + structure.getAssemblyIdentifier(r2) + "@" + structure.getLabelSelection(r2);
                    })
                    .forEach(System.out::println);
        }

        assertFalse(fail, "There are 0-distance residue pairs between residues in different assemblies");
    }

    @Test
    void whenFlippedDirectory_thenAngle180() {
        // pointing away
        float[] data = new float[] { 1, 0, 0, -1, 0, 0 };
        float angle = ResidueGraph.angle(data, 0, 1);
        assertEquals(180, angle, 0.001);
    }

    @Test
    void whenIdentical_thenAngle0() {
        // same
        float[] data = new float[] { 1, 0, 0, 1, 0, 0 };
        float angle = ResidueGraph.angle(data, 0, 1);
        assertEquals(0, angle, 0.001);
    }

    @Test
    void whenOrthogonalInXy_thenAngle90() {
        // right 1
        float[] data = new float[] { 1, 0, 0, 0, 1, 0 };
        float angle = ResidueGraph.angle(data, 0, 1);
        assertEquals(90, angle, 0.001);
    }

    @Test
    void whenOrthogonalInXz_thenAngle90() {
        // right 2
        float[] data = new float[] { 1, 0, 0, 0, 0, 1 };
        float angle = ResidueGraph.angle(data, 0, 1);
        assertEquals(90, angle, 0.001);
    }

    @Test
    void whenOrderSwapped_thenAngleSame() {
        for (int i = 0; i < 10; i++) {
            float[] v1 = new float[] { (float) Math.random(), (float) Math.random(), (float) Math.random() };
            Algebra.normalize3d(v1, v1);
            float[] v2 = new float[] { (float) Math.random(), (float) Math.random(), (float) Math.random() };
            Algebra.normalize3d(v2, v2);
            float[] data = new float[6];
            System.arraycopy(v1, 0, data, 0, v1.length);
            System.arraycopy(v2, 0, data, v1.length, v2.length);
            float a1 = ResidueGraph.angle(data, 0, 1);
            float a2 = ResidueGraph.angle(data, 1, 0);
            assertEquals(a1, a2, 0.001f, "order of angle calculation must not matter");
        }
    }
}