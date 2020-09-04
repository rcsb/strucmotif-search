package org.rcsb.strucmotif.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getResource;
import static org.rcsb.strucmotif.Helpers.honorTolerance;

public class MotifLookupImplTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() {
        // mock inverted index - register expected answers to select()
        this.invertedIndex = Mockito.mock(InvertedIndex.class);
        when(invertedIndex.select(any())).thenAnswer(this::select);
    }

    private Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(InvocationOnMock invocation) {
        ResiduePairDescriptor residuePairDescriptor = invocation.getArgument(0, ResiduePairDescriptor.class);

        try {
            InputStream inputStream = getResource("index/" + residuePairDescriptor.toString() + ".data");
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .map(this::handleLine);
        } catch (NullPointerException e) {
            // allowed to happen when empty bins are requested during operations with tolerance
            return Stream.empty();
        }
    }

    private Pair<StructureIdentifier, ResiduePairIdentifier[]> handleLine(String line) {
        String[] outerSplit = line.split(":");
        StructureIdentifier key = new StructureIdentifier(outerSplit[0]);
        ResiduePairIdentifier[] value = new ResiduePairIdentifier[outerSplit.length - 1];
        for (int i = 1; i < outerSplit.length; i++) {
            String[] innerSplit = outerSplit[i].split(",");
            int index1 = Integer.parseInt(innerSplit[0]);
            int index2 = Integer.parseInt(innerSplit[1]);
            int assemblyId1 = 1;
            int assemblyId2 = 1;
            if (innerSplit.length == 4) {
                assemblyId1 = Integer.parseInt(innerSplit[2]);
                assemblyId2 = Integer.parseInt(innerSplit[3]);
            }
            IndexSelection indexSelection1 = new IndexSelection(assemblyId1, index1);
            IndexSelection indexSelection2 = new IndexSelection(assemblyId2, index2);
            value[i - 1] = new ResiduePairIdentifier(indexSelection1, indexSelection2);
        }
        return new Pair<>(key, value);
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
    }
}