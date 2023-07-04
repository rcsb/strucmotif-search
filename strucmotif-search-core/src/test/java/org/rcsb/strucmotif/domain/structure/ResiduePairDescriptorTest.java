package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResiduePairDescriptorTest {
    @ParameterizedTest
    @MethodSource("shortCases")
    void whenRoundTrippingShortDescriptor_thenEquals(DistanceType d1, DistanceType d2, AngleType a) {
        assertShortDescriptorRoundTrips(d1, d2, a);
    }

    static Stream<Arguments> shortCases() {
        return Stream.of(Arguments.of(DistanceType.D0, DistanceType.D0, AngleType.A80),
                Arguments.of(DistanceType.D0, DistanceType.D0, AngleType.A160),
                Arguments.of(DistanceType.D4, DistanceType.D2, AngleType.A20),
                Arguments.of(DistanceType.D4, DistanceType.D2, AngleType.A80),
                Arguments.of(DistanceType.D4, DistanceType.D2, AngleType.A100),
                Arguments.of(DistanceType.D0, DistanceType.D4, AngleType.A40),
                Arguments.of(DistanceType.D0, DistanceType.D4, AngleType.A60),
                Arguments.of(DistanceType.D0, DistanceType.D4, AngleType.A180),
                Arguments.of(DistanceType.D8, DistanceType.D0, AngleType.A0),
                Arguments.of(DistanceType.D8, DistanceType.D0, AngleType.A100),
                Arguments.of(DistanceType.D8, DistanceType.D0, AngleType.A140),
                Arguments.of(DistanceType.D0, DistanceType.D8, AngleType.A180),
                Arguments.of(DistanceType.D0, DistanceType.D8, AngleType.A60),
                Arguments.of(DistanceType.D0, DistanceType.D8, AngleType.A80),
                Arguments.of(DistanceType.D16, DistanceType.D16, AngleType.A0),
                Arguments.of(DistanceType.D16, DistanceType.D16, AngleType.A100),
                Arguments.of(DistanceType.D16, DistanceType.D16, AngleType.A180),
                Arguments.of(DistanceType.D0, DistanceType.D24, AngleType.A20),
                Arguments.of(DistanceType.D0, DistanceType.D24, AngleType.A60),
                Arguments.of(DistanceType.D0, DistanceType.D24, AngleType.A160),
                Arguments.of(DistanceType.D31, DistanceType.D31, AngleType.A20),
                Arguments.of(DistanceType.D31, DistanceType.D31, AngleType.A80),
                Arguments.of(DistanceType.D31, DistanceType.D31, AngleType.A160),
                Arguments.of(DistanceType.D31, DistanceType.D31, AngleType.A180));
    }

    @ParameterizedTest
    @MethodSource("intCases")
    void whenRoundTrippingIntDescriptor_thenEquals(ResidueType r1, ResidueType r2, DistanceType d1, DistanceType d2, AngleType a) {
        assertIntDescriptorRoundTrips(r1, r2, d1, d2, a);
    }

    static Stream<Arguments> intCases() {
        return Stream.of(Arguments.of(ResidueType.ARGININE, ResidueType.TYROSINE, DistanceType.D0, DistanceType.D0, AngleType.A80),
                Arguments.of(ResidueType.ALANINE, ResidueType.URIDINE, DistanceType.D0, DistanceType.D0, AngleType.A160),
                Arguments.of(ResidueType.ALANINE, ResidueType.ALANINE, DistanceType.D4, DistanceType.D2, AngleType.A20),
                Arguments.of(ResidueType.UNKNOWN_COMPONENT, ResidueType.UNKNOWN_COMPONENT, DistanceType.D4, DistanceType.D2, AngleType.A80),
                Arguments.of(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, DistanceType.D4, DistanceType.D2, AngleType.A100),
                Arguments.of(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, DistanceType.D0, DistanceType.D4, AngleType.A40),
                Arguments.of(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, DistanceType.D0, DistanceType.D4, AngleType.A60),
                Arguments.of(ResidueType.ADENOSINE, ResidueType.GUANOSINE, DistanceType.D0, DistanceType.D4, AngleType.A180),
                Arguments.of(ResidueType.UNKNOWN_AMINO_ACID, ResidueType.UNKNOWN_NUCLEOTIDE, DistanceType.D8, DistanceType.D0, AngleType.A0),
                Arguments.of(ResidueType.SELENOCYSTEINE, ResidueType.VALINE, DistanceType.D8, DistanceType.D0, AngleType.A100),
                Arguments.of(ResidueType.PROLINE, ResidueType.TYROSINE, DistanceType.D8, DistanceType.D0, AngleType.A140),
                Arguments.of(ResidueType.ADENOSINE, ResidueType.CYSTEINE, DistanceType.D0, DistanceType.D8, AngleType.A180),
                Arguments.of(ResidueType.METHIONINE, ResidueType.PROLINE, DistanceType.D0, DistanceType.D8, AngleType.A60),
                Arguments.of(ResidueType.GLUTAMINE, ResidueType.VALINE, DistanceType.D0, DistanceType.D8, AngleType.A80),
                Arguments.of(ResidueType.GLYCINE, ResidueType.UNKNOWN_AMINO_ACID, DistanceType.D16, DistanceType.D16, AngleType.A0),
                Arguments.of(ResidueType.INOSINIC_ACID, ResidueType.ALANINE, DistanceType.D16, DistanceType.D16, AngleType.A100),
                Arguments.of(ResidueType.HISTIDINE, ResidueType.TYROSINE, DistanceType.D16, DistanceType.D16, AngleType.A180),
                Arguments.of(ResidueType.DEOXYCYTIDINE, ResidueType.INOSINIC_ACID, DistanceType.D0, DistanceType.D24, AngleType.A20),
                Arguments.of(ResidueType.ADENOSINE, ResidueType.PHENYLALANINE, DistanceType.D0, DistanceType.D24, AngleType.A60),
                Arguments.of(ResidueType.CYTIDINE, ResidueType.SELENOCYSTEINE, DistanceType.D0, DistanceType.D24, AngleType.A160),
                Arguments.of(ResidueType.GLYCINE, ResidueType.UNKNOWN_NUCLEOTIDE, DistanceType.D31, DistanceType.D31, AngleType.A20),
                Arguments.of(ResidueType.GLUTAMIC_ACID, ResidueType.UNKNOWN_AMINO_ACID, DistanceType.D31, DistanceType.D31, AngleType.A80),
                Arguments.of(ResidueType.CYSTEINE, ResidueType.ARGININE, DistanceType.D31, DistanceType.D31, AngleType.A160),
                Arguments.of(ResidueType.HISTIDINE, ResidueType.ISOLEUCINE, DistanceType.D31, DistanceType.D31, AngleType.A180));
    }

    private void assertShortDescriptorRoundTrips(DistanceType d1, DistanceType d2, AngleType a) {
        short s = ResiduePairDescriptor.encodeDescriptor(d1, d2, a);
        assertEquals(d1, ResiduePairDescriptor.getBackboneDistance(s));
        assertEquals(d2, ResiduePairDescriptor.getSideChainDistance(s));
        assertEquals(a, ResiduePairDescriptor.getAngle(s));
    }

    private void assertIntDescriptorRoundTrips(ResidueType r1, ResidueType r2, DistanceType d1, DistanceType d2, AngleType a) {
        int i = ResiduePairDescriptor.encodeDescriptor(r1, r2, d1, d2, a);
        assertEquals(r1, ResiduePairDescriptor.getResidueType1(i));
        assertEquals(r2, ResiduePairDescriptor.getResidueType2(i));
        assertEquals(d1, ResiduePairDescriptor.getBackboneDistance(i));
        assertEquals(d2, ResiduePairDescriptor.getSideChainDistance(i));
        assertEquals(a, ResiduePairDescriptor.getAngle(i));
    }
}
