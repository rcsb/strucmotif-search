package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Arrays;
import java.util.List;

/**
 * The result of a structural motif search.
 */
public record StructureHit(String structureIdentifier, String assemblyIdentifier, List<LabelSelection> labelSelections, List<ResidueType> residueTypes, float rootMeanSquareDeviation, float[] transformation) implements Hit {
    @Override
    public String toString() {
        return "StructureHit{" +
                "structureIdentifier='" + structureIdentifier + '\'' +
                ", assemblyIdentifier='" + assemblyIdentifier + '\'' +
                ", labelSelections=" + labelSelections +
                ", residueTypes=" + residueTypes +
                ", rootMeanSquareDeviation=" + rootMeanSquareDeviation +
                ", transformation=" + Arrays.toString(transformation) +
                '}';
    }
}
