package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

public interface Hit {
    // TODO generic identifier?

    List<LabelSelection> getLabelSelections();

    List<ResidueType> getResidueTypes();

    float getRootMeanSquareDeviation();

    Transformation getTransformation();
}
