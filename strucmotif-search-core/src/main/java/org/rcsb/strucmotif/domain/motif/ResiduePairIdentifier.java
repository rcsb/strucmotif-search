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

    public ResiduePairIdentifier(LabelSelection labelSelection1, LabelSelection labelSelection2) {
        this(labelSelection1, labelSelection2, null);
    }

    public ResiduePairIdentifier(LabelSelection labelSelection1, LabelSelection labelSelection2, ResiduePairDescriptor original) {
        this.labelSelection1 = labelSelection1;
        this.labelSelection2 = labelSelection2;
        this.score = original != null ? original.getScore() : null;
    }

    public LabelSelection getLabelSelection1() {
        return labelSelection1;
    }

    public LabelSelection getLabelSelection2() {
        return labelSelection2;
    }

    public GeometricDescriptorScore getScore() {
        return score;
    }

    public Stream<LabelSelection> labelSelections() {
        return Stream.of(labelSelection1, labelSelection2);
    }

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
