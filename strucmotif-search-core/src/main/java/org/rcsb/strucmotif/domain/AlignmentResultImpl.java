package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

/**
 * The result of an alignment.
 */
public class AlignmentResultImpl implements AlignmentResult {
    private final List<Residue> originalReference;
    private final List<Residue> originalCandidate;
    private final Transformation transformation;
    private final RootMeanSquareDeviation rootMeanSquareDeviation;

    /**
     * Construct an alignment result.
     * @param originalReference original reference residues
     * @param originalCandidate original candidate residues
     * @param transformation applied transformation
     * @param rootMeanSquareDeviation achieved score
     */
    public AlignmentResultImpl(List<Residue> originalReference, List<Residue> originalCandidate, Transformation transformation, RootMeanSquareDeviation rootMeanSquareDeviation) {
        this.originalReference = originalReference;
        this.originalCandidate = originalCandidate;
        this.transformation = transformation;
        this.rootMeanSquareDeviation = rootMeanSquareDeviation;
    }

    @Override
    public List<Residue> getOriginalReference() {
        return originalReference;
    }

    @Override
    public List<Residue> getOriginalCandidate() {
        return originalCandidate;
    }

    @Override
    public List<Residue> getAlignedCandidate() {
        return transformation.transformComponents(originalReference);
    }

    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public RootMeanSquareDeviation getRootMeanSquareDeviation() {
        return rootMeanSquareDeviation;
    }
}
