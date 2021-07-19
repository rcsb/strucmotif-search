package org.rcsb.strucmotif2.domain.result;

import org.rcsb.strucmotif2.domain.Transformation;
import org.rcsb.strucmotif2.domain.identifier.AssemblyIdentifier;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif2.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif2.domain.selection.LabelSelection;
import org.rcsb.strucmotif2.domain.structure.ResidueType;

import java.util.List;

/**
 * A hit with additional transformation information. Implemented using a {@link SimpleHit} as delegate.
 */
public class TransformedHit implements Hit {
    private final SimpleHit delegate;
    private final List<ResidueType> residueTypes;
    private final RootMeanSquareDeviation rootMeanSquareDeviation;
    private final Transformation transformation;

    /**
     * Construct a transformed hit (i.e., scored by an alignment).
     * @param delegate the wrapped simple hit
     * @param residueTypes mapped residue types
     * @param rootMeanSquareDeviation the alignment score
     * @param transformation the transformation determined by the alignment
     */
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
    public AssemblyIdentifier getAssemblyIdentifier() {
        return delegate.getAssemblyIdentifier();
    }

    @Override
    public List<LabelSelection> getSelection() {
        return delegate.getSelection();
    }

    @Override
    public GeometricDescriptorScore getGeometricDescriptorScore() {
        return delegate.getGeometricDescriptorScore();
    }

    /**
     * Returns residue types (label_comp_id) of this hit.
     * @return an ordered set of residue types
     */
    public List<ResidueType> getResidueTypes() {
        return residueTypes;
    }

    /**
     * Returns the RMSD of this hit.
     * @return a score object
     */
    public RootMeanSquareDeviation getRootMeanSquareDeviation() {
        return rootMeanSquareDeviation;
    }

    /**
     * Returns the transformation yielded by the underlying alignment.
     * @return a transformation object
     */
    public Transformation getTransformation() {
        return transformation;
    }
}
