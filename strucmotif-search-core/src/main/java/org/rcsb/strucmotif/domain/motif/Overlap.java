package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

/**
 * Describes the overlap between two {@link org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier} instances. Done by
 * determining whether {@link LabelSelection} instances equal. Can be no overlap or both overlapping. Interesting case
 * is when 1 pair overlaps. Can be LEFT_LEFT (left/first identifier of first word paired to left/first of second word)
 * and so on.
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
     * Determines the overlap between 2 {@link IndexSelectionResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(IndexSelectionResiduePairIdentifier residuePairIdentifier1, IndexSelectionResiduePairIdentifier residuePairIdentifier2) {
        IndexSelection indexSelection11 = residuePairIdentifier1.getIndexSelection1();
        IndexSelection indexSelection12 = residuePairIdentifier1.getIndexSelection2();
        IndexSelection indexSelection21 = residuePairIdentifier2.getIndexSelection1();
        IndexSelection indexSelection22 = residuePairIdentifier2.getIndexSelection2();

        boolean equal1112 = indexSelection11.equals(indexSelection21);
        boolean equal1122 = indexSelection11.equals(indexSelection22);
        boolean equal1221 = indexSelection12.equals(indexSelection21);
        boolean equal1222 = indexSelection12.equals(indexSelection22);
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

    /**
     * Determines the overlap between 2 {@link IndexSelectionResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
        boolean equal1112 = residuePairIdentifier1.getIndex1() == residuePairIdentifier2.getIndex1() && residuePairIdentifier1.getStructOperId1().equals(residuePairIdentifier2.getStructOperId1());
        boolean equal1122 = residuePairIdentifier1.getIndex1() == residuePairIdentifier2.getIndex2() && residuePairIdentifier1.getStructOperId1().equals(residuePairIdentifier2.getStructOperId2());
        boolean equal1221 = residuePairIdentifier1.getIndex2() == residuePairIdentifier2.getIndex1() && residuePairIdentifier1.getStructOperId2().equals(residuePairIdentifier2.getStructOperId1());
        boolean equal1222 = residuePairIdentifier1.getIndex2() == residuePairIdentifier2.getIndex2() && residuePairIdentifier1.getStructOperId2().equals(residuePairIdentifier2.getStructOperId2());
        return of(equal1112, equal1122, equal1221, equal1222);
    }
}
