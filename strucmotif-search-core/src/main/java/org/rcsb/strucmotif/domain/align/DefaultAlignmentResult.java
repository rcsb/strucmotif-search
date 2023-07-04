package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.domain.Transformation;

/**
 * The result of an alignment.
 */
public record DefaultAlignmentResult(Transformation transformation, float rmsd) implements AlignmentResult {
    // TODO interface needed?
    // TODO ensure somewhat acceptable naming of record getters
    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public float getRootMeanSquareDeviation() {
        return rmsd;
    }
}
