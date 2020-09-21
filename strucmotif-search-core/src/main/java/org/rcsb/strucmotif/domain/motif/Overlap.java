package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.selection.IndexSelection;

/**
 * Describes the overlap between two {@link ResiduePairIdentifier} instances. Done by determining whether
 * {@link IndexSelection} instances equal. Can be no overlap or both overlapping. Interesting case is when 1 pair
 * overlaps. Can be LEFT_LEFT (left/first identifier of first word paired to left/first of second word) and so on.
 */
public enum Overlap {
    NONE,
    LEFT_LEFT,
    LEFT_RIGHT,
    RIGHT_LEFT,
    RIGHT_RIGHT,
    BOTH;

    /**
     * Determines the overlap between 2 {@link ResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(ResiduePairIdentifier residuePairIdentifier1, ResiduePairIdentifier residuePairIdentifier2) {
        IndexSelection indexSelection1Left = residuePairIdentifier1.getIndexSelection1();
        int indexSelection1LeftAssemblyId = indexSelection1Left.getAssemblyId();
        int indexSelection1LeftIndex = indexSelection1Left.getIndex();
        IndexSelection indexSelection1Right = residuePairIdentifier1.getIndexSelection2();
        int indexSelection1RightAssemblyId = indexSelection1Right.getAssemblyId();
        int indexSelection1RightIndex = indexSelection1Right.getIndex();
        IndexSelection indexSelection2Left = residuePairIdentifier2.getIndexSelection1();
        int indexSelection2LeftAssemblyId = indexSelection2Left.getAssemblyId();
        int indexSelection2LeftIndex = indexSelection2Left.getIndex();
        IndexSelection indexSelection2Right = residuePairIdentifier2.getIndexSelection2();
        int indexSelection2RightAssemblyId = indexSelection2Right.getAssemblyId();
        int indexSelection2RightIndex = indexSelection2Right.getIndex();

        boolean equal1Left2Left = indexSelection1LeftAssemblyId == indexSelection2LeftAssemblyId &&
                indexSelection1LeftIndex == indexSelection2LeftIndex;
        boolean equal1Left2Right = indexSelection1LeftAssemblyId == indexSelection2RightAssemblyId &&
                indexSelection1LeftIndex == indexSelection2RightIndex;
        boolean equal2Left1Right = indexSelection1RightAssemblyId == indexSelection2LeftAssemblyId &&
                indexSelection1RightIndex == indexSelection2LeftIndex;
        boolean equal2Left2Right = indexSelection1RightAssemblyId == indexSelection2RightAssemblyId &&
                indexSelection1RightIndex == indexSelection2RightIndex;

        if (!equal1Left2Left && !equal1Left2Right && !equal2Left1Right && !equal2Left2Right) {
            return NONE;
        } else if ((equal1Left2Left && equal2Left2Right) || (equal1Left2Right && equal2Left1Right)) {
            return BOTH;
        } else if (equal1Left2Left) {
            return LEFT_LEFT;
        } else if (equal2Left2Right) {
            return RIGHT_RIGHT;
        } else if (equal1Left2Right) {
            return LEFT_RIGHT;
        } else {
            return RIGHT_LEFT;
        }
    }
}
