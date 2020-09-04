package org.rcsb.strucmotif.domain;

import org.junit.Test;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.IndexSelectionResolver;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.selection.LabelSelectionResolver;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.AllPurposeReader;
import org.rcsb.strucmotif.io.read.RenumberedReader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ResidueGraphTest {
    private AllPurposeReader allPurposeReader;
    private RenumberedReader renumberedReader;

    @Test
    public void shouldHandle3vvk() {
        Structure structure = renumberedReader.readById("3vvk");
        IndexSelectionResolver indexSelectionResolver = new IndexSelectionResolver(structure);
        LabelSelectionResolver labelSelectionResolver = new LabelSelectionResolver(structure);
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        long c = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(wordIdentifier -> Stream.of(wordIdentifier.getIndexSelection1(), wordIdentifier.getIndexSelection2()))
                // mapping shenanigans to determine label selection
                .map(indexSelectionResolver::resolve)
                .map(labelSelectionResolver::resolve)
                .distinct()
                .map(LabelSelection::getLabelAsymId)
                .distinct()
                .peek(System.out::println)
                .count();

        // this is a structure with distinct chains in altlocs
        // currently, there is no support for altlocs (especially of this nature) - thus, many weird words will be reported - that's okay for now
        assertEquals(6, c);
    }

    @Test
    public void shouldHandle1dsd() {
        Structure structure = renumberedReader.readById("1dsd");
        IndexSelectionResolver indexSelectionResolver = new IndexSelectionResolver(structure);
        LabelSelectionResolver labelSelectionResolver = new LabelSelectionResolver(structure);
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        long c = residueGraph.residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(wordIdentifier -> Stream.of(wordIdentifier.getIndexSelection1(), wordIdentifier.getIndexSelection2()))
                // mapping shenanigans to determine label selection
                .map(indexSelectionResolver::resolve)
                .map(labelSelectionResolver::resolve)
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
        assertEquals("unexpected number of valid components in chain C", 8, c);
    }

    @Test
    public void shouldReportMotifsFromGenericRead() {
        Structure structure = allPurposeReader.readById("200l");
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        assertEquals(5939, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getAssemblyId(), lookupTargetIdentifier.getIndexSelection2().getAssemblyId()))
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getIndex(), lookupTargetIdentifier.getIndexSelection2().getIndex()))
                .distinct()
                .count());
    }

    @Test
    public void shouldReportMotifsFromGenericReadWithAssemblies() {
        Structure structure = allPurposeReader.readById("1acj");
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        assertEquals(25230, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getAssemblyId(), lookupTargetIdentifier.getIndexSelection2().getAssemblyId()))
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getIndex(), lookupTargetIdentifier.getIndexSelection2().getIndex()))
                .distinct()
                .count());
    }

    @Test
    public void shouldReportMotifsFromHighThroughputRead() {
        Structure structure = renumberedReader.readById("200l");
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        assertEquals(5947,  residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(1, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getAssemblyId(), lookupTargetIdentifier.getIndexSelection2().getAssemblyId()))
                .distinct()
                .count());
        assertEquals(162, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getIndex(), lookupTargetIdentifier.getIndexSelection2().getIndex()))
                .distinct()
                .count());
    }

    @Test
    public void shouldReportMotifsFromHighThroughputReadWithAssemblies() {
        Structure structure = renumberedReader.readById("1acj");
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        assertEquals(25196, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .distinct()
                .count());
        assertEquals(2, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getAssemblyId(), lookupTargetIdentifier.getIndexSelection2().getAssemblyId()))
                .distinct()
                .count());
        assertEquals(528, residueGraph.residuePairOccurrencesParallel()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(lookupTargetIdentifier -> Stream.of(lookupTargetIdentifier.getIndexSelection1().getIndex(), lookupTargetIdentifier.getIndexSelection2().getIndex()))
                .distinct()
                .count());
    }

    private static final ResiduePairDescriptor ARGININE_TWEEZERS = new ResiduePairDescriptor(ResidueType.ARGININE,
            ResidueType.ARGININE,
            DistanceType.D15,
            DistanceType.D14,
            AngleType.A80);

    @Test
    public void shouldReportMotifsForArginineTweezers() {
        Structure structure = renumberedReader.readById("4ob8");
        List<ResiduePairDescriptor> residuePairDescriptors = honorTolerance(ARGININE_TWEEZERS).collect(Collectors.toList());
        ResidueGraph residueGraph = new ResidueGraph(structure, 400);

        residueGraph.residuePairOccurrencesParallel()
                .filter(wordOccurrence -> residuePairDescriptors.contains(wordOccurrence.getResiduePairDescriptor()))
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .forEach(System.out::println);
    }

    private Stream<ResiduePairDescriptor> honorTolerance(ResiduePairDescriptor residuePairDescriptor) {
        int alphaCarbonDistanceTolerance = 1;
        int sideChainDistanceTolerance = 1;
        int angleTolerance = 1;

        int alphaCarbonDistance = residuePairDescriptor.getBackboneDistance().ordinal();
        int sideChainDistance = residuePairDescriptor.getSideChainDistance().ordinal();
        int dihedralAngle = residuePairDescriptor.getAngle().ordinal();
        List<ResiduePairDescriptor> combinations = new ArrayList<>();

        for (int i = -alphaCarbonDistanceTolerance; i <= alphaCarbonDistanceTolerance; i++) {
            int ii = alphaCarbonDistance + i;
            if (ii < 0 || ii >= DistanceType.values().length) {
                continue;
            }

            for (int j = -sideChainDistanceTolerance; j <= sideChainDistanceTolerance; j++) {
                int ij = sideChainDistance + j;
                if (ij < 0 || ij >= DistanceType.values().length) {
                    continue;
                }

                for (int k = -angleTolerance; k <= angleTolerance; k++) {
                    int ik = dihedralAngle + k;
                    if (ik < 0 || ik >= AngleType.values().length) {
                        continue;
                    }

                    combinations.add(new ResiduePairDescriptor(residuePairDescriptor.getResidueType1(),
                            residuePairDescriptor.getResidueType2(),
                            DistanceType.values()[ii],
                            DistanceType.values()[ij],
                            AngleType.values()[ik]));
                }
            }
        }

        return combinations.stream();
    }

    @Test
    public void shouldComputeAngle() {
        // pointing away
        double angle1 = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { -1, 0, 0 });
        assertEquals(180, angle1, 0.001);

        // same
        double angle2 = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 1, 0, 0 });
        assertEquals(0, angle2, 0.001);

        // right 1
        double angle3 = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 0, 1, 0 });
        assertEquals(90, angle3, 0.001);

        // right 2
        double angle4 = ResidueGraph.angle(new double[] { 1, 0, 0 }, new double[] { 0, 0, 1 });
        assertEquals(90, angle4, 0.001);
    }
}
