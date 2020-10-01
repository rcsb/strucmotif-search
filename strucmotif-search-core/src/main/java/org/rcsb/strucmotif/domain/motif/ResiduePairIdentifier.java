package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Residue pair identifiers combine the {@link IndexSelection} of each residue making up this pair.
 */
public class ResiduePairIdentifier {
    private final IndexSelection indexSelection1;
    private final IndexSelection indexSelection2;
    private final LabelSelection labelSelection1;
    private final LabelSelection labelSelection2;
    private final int score;

    public ResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2, ResiduePairDescriptor original) {
        this(indexSelection1, indexSelection2, null, null, original);
    }

    public ResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2, LabelSelection labelSelection1, LabelSelection labelSelection2) {
        this(indexSelection1, indexSelection2, labelSelection1, labelSelection2, null);
    }

    public ResiduePairIdentifier(IndexSelection indexSelection1, IndexSelection indexSelection2, LabelSelection labelSelection1, LabelSelection labelSelection2, ResiduePairDescriptor original) {
        this.indexSelection1 = indexSelection1;
        this.indexSelection2 = indexSelection2;
        this.labelSelection1 = labelSelection1;
        this.labelSelection2 = labelSelection2;
        this.score = original != null ? original.getScore() : -1;
    }

    public IndexSelection getIndexSelection1() {
        return indexSelection1;
    }

    public IndexSelection getIndexSelection2() {
        return indexSelection2;
    }

    public LabelSelection getLabelSelection1() {
        return labelSelection1;
    }

    public LabelSelection getLabelSelection2() {
        return labelSelection2;
    }

    public int getScore() {
        return score;
    }

    public Stream<IndexSelection> indexSelections() {
        return Stream.of(indexSelection1, indexSelection2);
    }

    public boolean providesLabelSelection() {
        return labelSelection1 != null && labelSelection2 != null;
    }

    public boolean providesScore() {
        return score != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairIdentifier that = (ResiduePairIdentifier) o;
        return Objects.equals(indexSelection1, that.indexSelection1) &&
                Objects.equals(indexSelection2, that.indexSelection2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexSelection1, indexSelection2);
    }

    @Override
    public String toString() {
        return indexSelection1 + "+" + indexSelection2;
    }
}
