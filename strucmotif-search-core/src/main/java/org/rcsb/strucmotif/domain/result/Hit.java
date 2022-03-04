package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * One accepted hit.
 */
public interface Hit {
    /**
     * Referenced residues.
     * @return a collection of {@link LabelSelection}
     */
    List<LabelSelection> getLabelSelections();

    /**
     * Referenced residue types.
     * @return an ordered list of {@link ResidueType}
     */
    List<ResidueType> getResidueTypes();

    /**
     * The observed RMSD.
     * @return a float
     */
    float getRootMeanSquareDeviation();

    /**
     * The corresponding transformation.
     * @return a {@link Transformation} object
     */
    Transformation getTransformation();
}
