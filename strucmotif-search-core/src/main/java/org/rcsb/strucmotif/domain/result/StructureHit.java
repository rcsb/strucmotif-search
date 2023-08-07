package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Arrays;
import java.util.List;

/**
 * The result of a structural motif search.
 * @param structureIdentifier the entry ID
 * @param assemblyIdentifier the assembly ID
 * @param labelSelections selections for each matched residue
 * @param residueTypes mapped residue types
 * @param rootMeanSquareDeviation the alignment score
 * @param transformation the transformation determined by the alignment
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
