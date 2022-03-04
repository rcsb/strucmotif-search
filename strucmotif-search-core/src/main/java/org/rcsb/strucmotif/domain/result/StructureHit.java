package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * The result of a structural motif search.
 */
public class StructureHit implements Hit {
    private final String structureIdentifier;
    private final String assemblyIdentifier;
    private final List<LabelSelection> labelSelections;
    private final List<ResidueType> residueTypes;
    private final float rmsd;
    private final Transformation transformation;

    /**
     * Construct a transformed hit (i.e., scored by an alignment).
     * @param structureIdentifier the entry ID
     * @param assemblyIdentifier the assembly ID
     * @param labelSelections selections for each matched residue
     * @param residueTypes mapped residue types
     * @param rmsd the alignment score
     * @param transformation the transformation determined by the alignment
     */
    public StructureHit(String structureIdentifier,
                        String assemblyIdentifier,
                        List<LabelSelection> labelSelections,
                        List<ResidueType> residueTypes,
                        float rmsd,
                        Transformation transformation) {
        this.structureIdentifier = structureIdentifier;
        this.assemblyIdentifier = assemblyIdentifier;
        this.labelSelections = labelSelections;
        this.residueTypes = residueTypes;
        this.rmsd = rmsd;
        this.transformation = transformation;
    }

    /**
     * The structure identifier of this hit.
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * The assembly identifier of this hit.
     * @return a String, '0' if no assembly information was present and undefined assemblies are allowed
     */
    public String getAssemblyIdentifier() {
        return assemblyIdentifier;
    }

    @Override
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    @Override
    public List<ResidueType> getResidueTypes() {
        return residueTypes;
    }

    @Override
    public float getRootMeanSquareDeviation() {
        return rmsd;
    }

    @Override
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return "AssamHit{" +
                "structureIdentifier='" + structureIdentifier + '\'' +
                ", assemblyIdentifier='" + assemblyIdentifier + '\'' +
                ", labelSelections=" + labelSelections +
                ", residueTypes=" + residueTypes +
                ", rmsd=" + rmsd +
                ", transformation=" + transformation +
                '}';
    }
}
