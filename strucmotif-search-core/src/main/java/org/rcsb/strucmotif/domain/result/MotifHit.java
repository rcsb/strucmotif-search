package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * The result of a structural motif search.
 */
public class MotifHit implements Hit {
    private final String motifIdentifier;
    private final List<LabelSelection> labelSelections;
    private final List<ResidueType> residueTypes;
    private final float rmsd;
    private final Transformation transformation;

    /**
     * Construct a transformed hit (i.e., scored by an alignment).
     * @param motifIdentifier the motif ID
     * @param labelSelections selections for each matched residue
     * @param residueTypes mapped residue types
     * @param rmsd the alignment score
     * @param transformation the transformation determined by the alignment
     */
    public MotifHit(String motifIdentifier,
                    List<LabelSelection> labelSelections,
                    List<ResidueType> residueTypes,
                    float rmsd,
                    Transformation transformation) {
        this.motifIdentifier = motifIdentifier;
        this.labelSelections = labelSelections;
        this.residueTypes = residueTypes;
        this.rmsd = rmsd;
        this.transformation = transformation;
    }

    /**
     * The motif identifier of this hit.
     * @return a String
     */
    public String getMotifIdentifier() {
        return motifIdentifier;
    }

    /**
     * Referenced residues.
     * @return a collection of {@link LabelSelection}
     */
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    /**
     * Referenced residue types.
     * @return an ordered list of {@link ResidueType}
     */
    public List<ResidueType> getResidueTypes() {
        return residueTypes;
    }

    /**
     * The observed RMSD.
     * @return a float
     */
    public float getRootMeanSquareDeviation() {
        return rmsd;
    }

    /**
     * The corresponding transformation.
     * @return a {@link Transformation} object
     */
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return "MotifHit{" +
                "motifIdentifier='" + motifIdentifier + '\'' +
                ", labelSelections=" + labelSelections +
                ", residueTypes=" + residueTypes +
                ", rmsd=" + rmsd +
                ", transformation=" + transformation +
                '}';
    }
}
