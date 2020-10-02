package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

public class TransformedHit implements Hit {
    private final SimpleHit delegate;
    private final List<ResidueType> residueTypes;
    private final RootMeanSquareDeviation rootMeanSquareDeviation;
    private final Transformation transformation;

    public TransformedHit(SimpleHit delegate, List<ResidueType> residueTypes, RootMeanSquareDeviation rootMeanSquareDeviation, Transformation transformation) {
        this.delegate = delegate;
        this.residueTypes = residueTypes;
        this.rootMeanSquareDeviation = rootMeanSquareDeviation;
        this.transformation = transformation;
    }

    @Override
    public StructureIdentifier getStructureIdentifier() {
        return delegate.getStructureIdentifier();
    }

    @Override
    public List<LabelSelection> getSelection() {
        return delegate.getSelection();
    }

    @Override
    public GeometricDescriptorScore getGeometricDescriptorScore() {
        return delegate.getGeometricDescriptorScore();
    }

    public List<ResidueType> getResidueTypes() {
        return residueTypes;
    }

    public RootMeanSquareDeviation getRootMeanSquareDeviation() {
        return rootMeanSquareDeviation;
    }

    public Transformation getTransformation() {
        return transformation;
    }
}
