package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a hit. Tries to be sparse with the provided information but captures everything needed to 'recreate' this
 * hit (i.e. structure identifiers, component identifiers, and transformation).
 */
public class Hit {
    private final StructureIdentifier structureIdentifier;
    private final String title;
    private final LabelSelection[] selection;
    private final ResidueType[] residueTypes;
    private final double rootMeanSquareDeviation;
    private final double[] transformation;

    public Hit(StructureIdentifier structureIdentifier,
               String title,
               LabelSelection[] selection,
               ResidueType[] residueTypes,
               double rootMeanSquareDeviation,
               double[] transformation) {
        this.structureIdentifier = structureIdentifier;
        this.title = title;
        this.selection = selection;
        this.residueTypes = residueTypes;
        this.rootMeanSquareDeviation = rootMeanSquareDeviation;
        this.transformation = transformation;
    }

    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    public String getTitle() {
        return title;
    }

    public LabelSelection[] getSelection() {
        return selection;
    }

    public ResidueType[] getResidueTypes() {
        return residueTypes;
    }

    public double getRootMeanSquareDeviation() {
        return rootMeanSquareDeviation;
    }

    public double[] getTransformation() {
        return transformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hit hit = (Hit) o;
        return Double.compare(hit.rootMeanSquareDeviation, rootMeanSquareDeviation) == 0 &&
                Objects.equals(structureIdentifier, hit.structureIdentifier) &&
                Arrays.equals(selection, hit.selection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(structureIdentifier, selection, rootMeanSquareDeviation);
    }

    @Override
    public String toString() {
        return structureIdentifier + " " + Arrays.toString(selection) + " (" + Arrays.stream(residueTypes).map(ResidueType::getThreeLetterCode).collect(Collectors.toList()) + ") " + MotifSearch.format(rootMeanSquareDeviation);
    }
}
