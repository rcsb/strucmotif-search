package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Residue pair identifiers combine the {@link LabelSelection} of each residue making up this pair.
 */
public class ResiduePairIdentifier {
    private final LabelSelection labelSelection1;
    private final LabelSelection labelSelection2;
    private final GeometricDescriptorScore score;

    /**
     * Construct an identifier pair.
     * @param labelSelection1 first selection
     * @param labelSelection2 second selection
     */
    public ResiduePairIdentifier(LabelSelection labelSelection1, LabelSelection labelSelection2) {
        this(labelSelection1, labelSelection2, null);
    }

    /**
     * Construct an identifier pair.
     * @param labelSelection1 first selection
     * @param labelSelection2 second selection
     * @param original if present the score of this reference descriptor will be attached to this instance
     */
    public ResiduePairIdentifier(LabelSelection labelSelection1, LabelSelection labelSelection2, ResiduePairDescriptor original) {
        this.labelSelection1 = labelSelection1;
        this.labelSelection2 = labelSelection2;
        this.score = original != null ? original.getScore() : null;
    }

    /**
     * First selection.
     * @return a label selection
     */
    public LabelSelection getLabelSelection1() {
        return labelSelection1;
    }

    /**
     * Second selection.
     * @return a label selection
     */
    public LabelSelection getLabelSelection2() {
        return labelSelection2;
    }

    /**
     * The score of this occurrence which will be used to derive the complete geometric descriptor score of a putative
     * hit.
     * @return a score or null
     */
    public GeometricDescriptorScore getScore() {
        return score;
    }

    /**
     * Convenience method to access referenced selections.
     * @return Stream of both selections
     */
    public Stream<LabelSelection> labelSelections() {
        return Stream.of(labelSelection1, labelSelection2);
    }

    /**
     * States if this instances references a score.
     * @return true if score is attached
     */
    public boolean providesScore() {
        return score != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairIdentifier that = (ResiduePairIdentifier) o;
        return Objects.equals(labelSelection1, that.labelSelection1) &&
                Objects.equals(labelSelection2, that.labelSelection2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelSelection1, labelSelection2);
    }

    @Override
    public String toString() {
        return labelSelection1 + "+" + labelSelection2;
    }
}
