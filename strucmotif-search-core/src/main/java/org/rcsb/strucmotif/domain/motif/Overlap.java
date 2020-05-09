package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.selection.IndexSelection;

/**
 * Describes the overlap between two {@link ResiduePairIdentifier} instances. Done by determining whether {@link IndexSelection}
 * instances equal. Can be no overlap or both overlapping. Interesting case is when 1 pair overlaps. Can be LEFT_LEFT
 * (left/first identifier of first word paired to left/first of second word) and so on.
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
        IndexSelection uei11 = residuePairIdentifier1.getIndexSelection1();
        int uei11a = uei11.getAssemblyId();
        int uei11i = uei11.getIndex();
        IndexSelection uei12 = residuePairIdentifier1.getIndexSelection2();
        int uei12a = uei12.getAssemblyId();
        int uei12i = uei12.getIndex();
        IndexSelection uei21 = residuePairIdentifier2.getIndexSelection1();
        int uei21a = uei21.getAssemblyId();
        int uei21i = uei21.getIndex();
        IndexSelection uei22 = residuePairIdentifier2.getIndexSelection2();
        int uei22a = uei22.getAssemblyId();
        int uei22i = uei22.getIndex();

        boolean e1121 = uei11a == uei21a && uei11i == uei21i;
        boolean e1122 = uei11a == uei22a && uei11i == uei22i;
        boolean e1221 = uei12a == uei21a && uei12i == uei21i;
        boolean e1222 = uei12a == uei22a && uei12i == uei22i;

        if (!e1121 && !e1122 && !e1221 && !e1222) {
            return NONE;
        } else if ((e1121 && e1222) || (e1122 && e1221)) {
            return BOTH;
        } else if (e1121) {
            return LEFT_LEFT;
        } else if (e1222) {
            return RIGHT_RIGHT;
        } else if (e1122) {
            return LEFT_RIGHT;
        } else {
            return RIGHT_LEFT;
        }
    }
}
