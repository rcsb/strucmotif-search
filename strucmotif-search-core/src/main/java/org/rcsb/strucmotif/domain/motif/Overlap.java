package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.selection.LabelSelection;

/**
 * Describes the overlap between two {@link ResiduePairIdentifier} instances. Done by determining whether
 * {@link LabelSelection} instances equal. Can be no overlap or both overlapping. Interesting case is when 1 pair
 * overlaps. Can be LEFT_LEFT (left/first identifier of first word paired to left/first of second word) and so on.
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
     * Both sides overlap.
     */
    BOTH;

    /**
     * Determines the overlap between 2 {@link ResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(ResiduePairIdentifier residuePairIdentifier1, ResiduePairIdentifier residuePairIdentifier2) {
        LabelSelection indexSelection1Left = residuePairIdentifier1.getLabelSelection1();
        LabelSelection indexSelection1Right = residuePairIdentifier1.getLabelSelection2();
        LabelSelection indexSelection2Left = residuePairIdentifier2.getLabelSelection1();
        LabelSelection indexSelection2Right = residuePairIdentifier2.getLabelSelection2();

        boolean equal1Left2Left = indexSelection1Left.equals(indexSelection2Left);
        boolean equal1Left2Right = indexSelection1Left.equals(indexSelection2Right);
        boolean equal2Left1Right = indexSelection1Right.equals(indexSelection2Left);
        boolean equal2Left2Right = indexSelection1Right.equals(indexSelection2Right);

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
