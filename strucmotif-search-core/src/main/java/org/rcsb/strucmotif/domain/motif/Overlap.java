package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

/**
 * Describes the overlap between two {@link org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier} instances. Done by determining whether
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
     * Determines the overlap between 2 {@link LabelSelectionResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(LabelSelectionResiduePairIdentifier residuePairIdentifier1, LabelSelectionResiduePairIdentifier residuePairIdentifier2) {
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

    /**
     * Determines the overlap between 2 {@link InvertedIndexResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
        // TODO optimize?
        boolean equal1Left2Left = residuePairIdentifier1.getLabelSeqId1() == residuePairIdentifier2.getLabelSeqId1() &&
                residuePairIdentifier1.getLabelAsymId1().equals(residuePairIdentifier2.getLabelAsymId1()) &&
                residuePairIdentifier1.getStructOperId1().equals(residuePairIdentifier2.getStructOperId1());
        boolean equal1Left2Right = residuePairIdentifier1.getLabelSeqId1() == residuePairIdentifier2.getLabelSeqId2() &&
                residuePairIdentifier1.getLabelAsymId1().equals(residuePairIdentifier2.getLabelAsymId2()) &&
                residuePairIdentifier1.getStructOperId1().equals(residuePairIdentifier2.getStructOperId2());
        boolean equal2Left1Right = residuePairIdentifier1.getLabelSeqId2() == residuePairIdentifier2.getLabelSeqId1() &&
                residuePairIdentifier1.getLabelAsymId2().equals(residuePairIdentifier2.getLabelAsymId1()) &&
                residuePairIdentifier1.getStructOperId2().equals(residuePairIdentifier2.getStructOperId1());
        boolean equal2Left2Right = residuePairIdentifier1.getLabelSeqId2() == residuePairIdentifier2.getLabelSeqId2() &&
                residuePairIdentifier1.getLabelAsymId2().equals(residuePairIdentifier2.getLabelAsymId2()) &&
                residuePairIdentifier1.getStructOperId2().equals(residuePairIdentifier2.getStructOperId2());

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
