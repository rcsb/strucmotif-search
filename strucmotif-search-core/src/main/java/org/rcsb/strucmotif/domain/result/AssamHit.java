package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * The result of a structural motif search.
 */
public class AssamHit implements Hit {
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
    public AssamHit(String structureIdentifier,
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

    /**
     * Get all label selections of this hit.
     * @return a collection of label selections
     */
    @Override
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    /**
     * Returns residue types (label_comp_id) of this hit.
     * @return an ordered set of residue types
     */
    @Override
    public List<ResidueType> getResidueTypes() {
        return residueTypes;
    }

    /**
     * Returns the RMSD of this hit.
     * @return a float
     */
    @Override
    public float getRootMeanSquareDeviation() {
        return rmsd;
    }

    /**
     * Returns the transformation yielded by the underlying alignment.
     * @return a transformation object
     */
    @Override
    public Transformation getTransformation() {
        return transformation;
    }
}
