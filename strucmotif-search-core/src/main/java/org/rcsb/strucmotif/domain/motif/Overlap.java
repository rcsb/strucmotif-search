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
     * Determines the overlap between 2 {@link ResiduePairIdentifier} instances.
     * @param residuePairIdentifier1 the first instance
     * @param residuePairIdentifier2 the second instance
     * @return a description of the observed overlap
     */
    public static Overlap ofResiduePairIdentifiers(ResiduePairIdentifier residuePairIdentifier1, ResiduePairIdentifier residuePairIdentifier2) {
        // TODO
        return null;
    }
}
