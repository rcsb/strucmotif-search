package org.rcsb.strucmotif.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rcsb.strucmotif.Helpers.honorTolerance;

public class InvertedIndexImplTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() {
        this.invertedIndex = Helpers.INVERTED_INDEX;
    }

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
    public void whenAccessingSpecificBin_thenObserveAssemblies() {
        assertTrue(invertedIndex.select(BIN_WITH_ASSEMBLY)
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
    public void whenAccessingArginineTweezerBin_thenObserveAssemblies() {
        Map<StructureIdentifier, ResiduePairIdentifier[]> map = honorTolerance(ARGININE_TWEEZERS)
                .flatMap(invertedIndex::select)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, InvertedIndexImplTest::concat));

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
    }
}