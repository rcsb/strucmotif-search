package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rcsb.strucmotif.GuiceJUnit4Runner;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

@RunWith(GuiceJUnit4Runner.class)
public class MotifLookupImplTest {
    @Inject
    private InvertedIndex motifLookup;
    private static final ResiduePairDescriptor ARGININE_TWEEZERS = new ResiduePairDescriptor(ResidueType.ARGININE,
            ResidueType.ARGININE,
            DistanceType.D15,
            DistanceType.D14,
            AngleType.A80);
    private static final ResiduePairDescriptor BIN_WITH_ASSEMBLY = new ResiduePairDescriptor(ResidueType.ALANINE,
            ResidueType.VALINE,
            DistanceType.D20,
            DistanceType.D20,
            AngleType.A100);

    @Test
    public void shouldReportAssembliesForRandomExample() {
        assertTrue(motifLookup.select(BIN_WITH_ASSEMBLY)
                .map(Pair::getSecond)
                .flatMap(Arrays::stream)
                .flatMap(wordIdentifier -> Stream.of(wordIdentifier.getIndexSelection1(), wordIdentifier.getIndexSelection2()))
                .anyMatch(indexSelector -> indexSelector.getAssemblyId() > 1));
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    @Test
    public void shouldReportAssembliesForArginineTweezers() {
        Map<StructureIdentifier, ResiduePairIdentifier[]> map = honorTolerance(ARGININE_TWEEZERS)
                .flatMap(motifLookup::select)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, MotifLookupImplTest::concat));

        // assert that assemblies unequal to 1 exist
        assertTrue(map.values()
                .stream()
                .flatMap(Arrays::stream)
                .flatMap(wordIdentifier -> Stream.of(wordIdentifier.getIndexSelection1(), wordIdentifier.getIndexSelection2()))
                .anyMatch(indexSelector -> indexSelector.getAssemblyId() > 1));

        // assert that every occurrence has two identifiers
        assertTrue(map.values()
                .stream()
                .flatMap(Arrays::stream)
                .map(Objects::toString)
                .noneMatch(id -> id.split("\\+")[1].length() < 3));

        // investigate what happens for a failing structure
        Arrays.stream(map.get(new StructureIdentifier("4ob8")))
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
}