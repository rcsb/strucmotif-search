package org.rcsb.strucmotif.domain.motif;

/**
 * Can be no overlap or both overlapping. Interesting case is when 1 pair overlaps. Can be LEFT_LEFT (left/first
 * identifier of first word paired to left/first of second word) and so on.
 */
public enum Overlap {
    /**
     * No overlap.
     */
    NONE,
    /**
     * Left identifier of first word corresponds to left identifier of second word.
     */
    LEFT_LEFT,
    /**
     * Left identifier of first word corresponds to right identifier of second word.
     */
    LEFT_RIGHT,
    /**
     * Right identifier of first word corresponds to left identifier of second word.
     */
    RIGHT_LEFT,
    /**
     * Right identifier of first word corresponds to right identifier of second word.
     */
    RIGHT_RIGHT,
    /**
     * The degenerate case where both sides are referencing the same residues.
     */
    BOTH;

    /**
     * Determines the overlap between 2 residue pair identifiers.
     * @param r11 1st index of 1st identifier
     * @param r12 2nd index of 1st identifier
     * @param r21 1st index of 2nd identifier
     * @param r22 2nd index of 2nd identifier
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(int r11, int r12, int r21, int r22) {
        boolean equal1112 = r11 == r21;
        boolean equal1122 = r11 == r22;
        boolean equal1221 = r12 == r21;
        boolean equal1222 = r12 == r22;
        return of(equal1112, equal1122, equal1221, equal1222);
    }

    /**
     * Determines the overlap between 2 residue pair identifiers.
     * @param r1 1st identifier
     * @param r2 2nd identifier
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(long r1, long r2) {
        int r11 = ResiduePairIdentifier.getResidueIndex1(r1);
        int r12 = ResiduePairIdentifier.getResidueIndex2(r1);
        int r21 = ResiduePairIdentifier.getResidueIndex1(r2);
        int r22 = ResiduePairIdentifier.getResidueIndex2(r2);
        boolean equal1112 = r11 == r21;
        boolean equal1122 = r11 == r22;
        boolean equal1221 = r12 == r21;
        boolean equal1222 = r12 == r22;
        return of(equal1112, equal1122, equal1221, equal1222);
    }

    private static Overlap of(boolean equal1112, boolean equal1122, boolean equal1221, boolean equal1222) {
        if (!equal1112 && !equal1122 && !equal1221 && !equal1222) {
            return NONE;
        } else if ((equal1112 && equal1222) || (equal1122 && equal1221)) {
            return BOTH;
        } else if (equal1112) {
            return LEFT_LEFT;
        } else if (equal1222) {
            return RIGHT_RIGHT;
        } else if (equal1122) {
            return LEFT_RIGHT;
        } else {
            return RIGHT_LEFT;
        }
    }
}
