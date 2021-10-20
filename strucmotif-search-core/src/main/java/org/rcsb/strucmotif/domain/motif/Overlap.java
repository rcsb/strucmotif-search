package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.function.BiPredicate;

/**
 * Describes the overlap between two {@link org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier} instances. Done by determining whether
 * {@link LabelSelection} instances equal. Can be no overlap or both overlapping. Interesting case is when 1 pair
 * overlaps. Can be LEFT_LEFT (left/first identifier of first word paired to left/first of second word) and so on.
 */
public enum Overlap implements BiPredicate<InvertedIndexResiduePairIdentifier, InvertedIndexResiduePairIdentifier> {
    /**
     * No overlap.
     */
    NONE {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection11 = residuePairIdentifier1.getIndexSelection1();
            IndexSelection indexSelection12 = residuePairIdentifier1.getIndexSelection2();
            IndexSelection indexSelection21 = residuePairIdentifier2.getIndexSelection1();
            IndexSelection indexSelection22 = residuePairIdentifier2.getIndexSelection2();
            return !testInternal(indexSelection11, indexSelection21) && !testInternal(indexSelection12, indexSelection22) &&
                    !testInternal(indexSelection11, indexSelection22) && !testInternal(indexSelection12, indexSelection21);
        }
    },
    /**
     * Left identifier of first word corresponds to left identifier of second word.
     */
    LEFT_LEFT {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection11 = residuePairIdentifier1.getIndexSelection1();
            IndexSelection indexSelection21 = residuePairIdentifier2.getIndexSelection1();
            return testInternal(indexSelection11, indexSelection21);
        }
    },
    /**
     * Left identifier of first word corresponds to right identifier of second word.
     */
    LEFT_RIGHT {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection11 = residuePairIdentifier1.getIndexSelection1();
            IndexSelection indexSelection22 = residuePairIdentifier2.getIndexSelection2();
            return testInternal(indexSelection11, indexSelection22);
        }
    },
    /**
     * Right identifier of first word corresponds to left identifier of second word.
     */
    RIGHT_LEFT {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection12 = residuePairIdentifier1.getIndexSelection2();
            IndexSelection indexSelection21 = residuePairIdentifier2.getIndexSelection1();
            return testInternal(indexSelection12, indexSelection21);
        }
    },
    /**
     * Right identifier of first word corresponds to right identifier of second word.
     */
    RIGHT_RIGHT {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection12 = residuePairIdentifier1.getIndexSelection2();
            IndexSelection indexSelection22 = residuePairIdentifier2.getIndexSelection2();
            return testInternal(indexSelection12, indexSelection22);
        }
    },
    /**
     * Both sides overlap.
     */
    BOTH {
        @Override
        public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
            IndexSelection indexSelection11 = residuePairIdentifier1.getIndexSelection1();
            IndexSelection indexSelection12 = residuePairIdentifier1.getIndexSelection2();
            IndexSelection indexSelection21 = residuePairIdentifier2.getIndexSelection1();
            IndexSelection indexSelection22 = residuePairIdentifier2.getIndexSelection2();
            return (testInternal(indexSelection11, indexSelection21) && testInternal(indexSelection12, indexSelection22)) ||
                    (testInternal(indexSelection11, indexSelection22) && testInternal(indexSelection12, indexSelection21));
        }
    };

    private static boolean testInternal(IndexSelection indexSelection1, IndexSelection indexSelection2) {
        return indexSelection1.getIndex() == indexSelection2.getIndex() &&
                indexSelection1.getStructOperId().equals(indexSelection2.getStructOperId());
    }

    @Override
    public boolean test(InvertedIndexResiduePairIdentifier residuePairIdentifier1, InvertedIndexResiduePairIdentifier residuePairIdentifier2) {
        throw new UnsupportedOperationException("Not implemented");
    }

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
