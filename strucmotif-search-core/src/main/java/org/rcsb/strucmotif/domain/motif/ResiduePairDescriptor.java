package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.ResidueType;

/**
 * A residue pair descriptor tracks the properties defined between 2 residues. These values are the Cartesian product of
 * ResidueType (A, 36 states, 6 bits) x ResidueType (B, 36 states, 6 bits) x DistanceType (C, 32 states, 5 bits) x
 * DistanceType (D, 32 states, 5 bits) x AngleType (E, 10 states, 4 bits) and are stored in an unsigned 32-bit integer.
 * A second flavor exists that only tracks DistanceType x DistanceType x AngleType and can be held in an unsigned 16-bit
 * short.<p>
 *
 * int flavor: <pre>XXXMAAAA AABBBBBB XXCCCCCD DDDDEEEE</pre>
 * short flavor: <pre>XXCCCCCD DDDDEEEE</pre>
 *
 * The 32-bit descriptors will use their 4th bit to store metadata (M) that tracks whether the identifier is flipped.
 */
public class ResiduePairDescriptor {
    static final int FLIPPED_MASK = 1 << 28; // 4-th bit from the left active

    public static short encodeDescriptor(DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        return encodeDescriptor(backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal());
    }

    public static short encodeDescriptor(int backboneDistanceOrdinal, int sideChainDistanceOrdinal, int angleOrdinal) {
        return (short) (backboneDistanceOrdinal << 9 | sideChainDistanceOrdinal << 4 | angleOrdinal);
    }

    public static int encodeDescriptor(ResidueType residueType1, ResidueType residueType2, DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        return encodeDescriptor(residueType1.ordinal(), residueType2.ordinal(), backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal());
    }

    public static int encodeDescriptor(int residueTypeOrdinal1, int residueTypeOrdinal2, int backboneDistanceOrdinal, int sideChainDistanceOrdinal, int angleOrdinal) {
        return residueTypeOrdinal1 << 22 | residueTypeOrdinal2 << 16 | backboneDistanceOrdinal << 9 | sideChainDistanceOrdinal << 4 | angleOrdinal;
    }

    public static ResidueType getResidueType1(int descriptor) {
        return ResidueType.values()[descriptor >>> 22 & 0x3F];
    }

    public static ResidueType getResidueType2(int descriptor) {
        return ResidueType.values()[descriptor >>> 16 & 0x3F];
    }

    public static DistanceType getBackboneDistance(int descriptor) {
        return DistanceType.values()[descriptor >>> 9 & 0x1F];
    }

    public static DistanceType getSideChainDistance(int descriptor) {
        return DistanceType.values()[descriptor >>> 4 & 0x1F];
    }

    public static AngleType getAngle(int descriptor) {
        return AngleType.values()[descriptor & 0x0F];
    }

    /**
     * A descriptor is ambiguous if both residue types are the same.
     * @param descriptor descriptor to evaluate
     * @return true if both residue types match
     */
    public static boolean isAmbiguous(int descriptor) {
        return (descriptor >>> 22 & 0x3F) == (descriptor >>> 16 & 0x3F);
    }

    /**
     * An occurrence is flipped if it was implicitly rearrange to accommodate the contract of how descriptors are
     * composed (the lower residue type must occur first).
     * @param descriptor descriptor to evaluate
     * @return true if the descriptor has the corresponding metadata set to true
     */
    public static boolean isFlipped(int descriptor) {
        return (descriptor & FLIPPED_MASK) != 0;
    }
}
