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
    public static final int FLIPPED_MASK = 1 << 28; // 4-th bit from the left active

    /**
     * Encode (backboneDistance, sideChainDistance, angle) as short.
     * @param backboneDistance v1
     * @param sideChainDistance v2
     * @param angle v3
     * @return encoded as short
     */
    public static short encodeDescriptor(DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        return encodeDescriptor(backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal());
    }

    /**
     * Encode (backboneDistance, sideChainDistance, angle) as short.
     * @param backboneDistanceOrdinal v1
     * @param sideChainDistanceOrdinal v2
     * @param angleOrdinal v3
     * @return encoded as short
     */
    public static short encodeDescriptor(int backboneDistanceOrdinal, int sideChainDistanceOrdinal, int angleOrdinal) {
        return (short) (backboneDistanceOrdinal << 9 | sideChainDistanceOrdinal << 4 | angleOrdinal);
    }

    /**
     * Encode (residueType1, residueType2, backboneDistance, sideChainDistance, angle) as int.
     * @param residueType1 v1
     * @param residueType2 v2
     * @param backboneDistance v3
     * @param sideChainDistance v4
     * @param angle v5
     * @return encoded as int
     */
    public static int encodeDescriptor(ResidueType residueType1, ResidueType residueType2, DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        return encodeDescriptor(residueType1.ordinal(), residueType2.ordinal(), backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal());
    }

    /**
     * Encode (residueType1, residueType2, backboneDistance, sideChainDistance, angle) as int.
     * @param residueTypeOrdinal1 v1
     * @param residueTypeOrdinal2 v2
     * @param backboneDistanceOrdinal v3
     * @param sideChainDistanceOrdinal v4
     * @param angleOrdinal v5
     * @return encoded as int
     */
    public static int encodeDescriptor(int residueTypeOrdinal1, int residueTypeOrdinal2, int backboneDistanceOrdinal, int sideChainDistanceOrdinal, int angleOrdinal) {
        return residueTypeOrdinal1 << 22 | residueTypeOrdinal2 << 16 | backboneDistanceOrdinal << 9 | sideChainDistanceOrdinal << 4 | angleOrdinal;
    }

    /**
     * Extract the 1st residue type from an encoded descriptor.
     * @param descriptor int descriptor
     * @return the 1st type
     */
    public static ResidueType getResidueType1(int descriptor) {
        return ResidueType.values[descriptor >>> 22 & 0x3F];
    }

    /**
     * Extract the 2nd residue type from an encoded descriptor.
     * @param descriptor int descriptor
     * @return the 2nd type
     */
    public static ResidueType getResidueType2(int descriptor) {
        return ResidueType.values[descriptor >>> 16 & 0x3F];
    }

    /**
     * Extract the backbone distance type from an encoded descriptor.
     * @param descriptor short/int descriptor
     * @return the backbone distance type
     */
    public static DistanceType getBackboneDistance(int descriptor) {
        return DistanceType.values[descriptor >>> 9 & 0x1F];
    }

    /**
     * Extract the side-chain distance type from an encoded descriptor.
     * @param descriptor short/int descriptor
     * @return the side-chain distance type
     */
    public static DistanceType getSideChainDistance(int descriptor) {
        return DistanceType.values[descriptor >>> 4 & 0x1F];
    }

    /**
     * Extract the angle type from an encoded descriptor.
     * @param descriptor short/int descriptor
     * @return the angle type
     */
    public static AngleType getAngle(int descriptor) {
        return AngleType.values[descriptor & 0x0F];
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

    /**
     * Pretty-print an int descriptor.
     * @param descriptor int descriptor
     * @return human-readable String
     */
    public static String toString(int descriptor) {
        return getResidueType1(descriptor).getInternalCode() +
                getResidueType2(descriptor).getInternalCode() + "-" +
                getBackboneDistance(descriptor).ordinal() + "-" +
                getSideChainDistance(descriptor).ordinal() + "-" +
                getAngle(descriptor).ordinal();
    }

    /**
     * Pretty-print a short descriptor.
     * @param descriptor short descriptor
     * @return human-readable String
     */
    public static String toString(short descriptor) {
        return getBackboneDistance(descriptor).ordinal() + "-" +
                getSideChainDistance(descriptor).ordinal() + "-" +
                getAngle(descriptor).ordinal();
    }

    /**
     * Remove the flip-bit from an int descriptor.
     * @param descriptor int descriptor
     * @return the same descriptor with the flip bit inactive
     */
    public static int stripFlipBit(int descriptor) {
        return descriptor & ~FLIPPED_MASK;
    }
}
