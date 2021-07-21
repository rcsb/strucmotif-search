package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.score.RootMeanSquareDeviation;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * The result of a structural motif search.
 */
public class Hit {
    private final String structureIdentifier;
    private final String assemblyIdentifier;
    private final List<LabelSelection> labelSelections;
    private final List<ResidueType> residueTypes;
    private final RootMeanSquareDeviation rootMeanSquareDeviation;
    private final Transformation transformation;

    /**
     * Construct a transformed hit (i.e., scored by an alignment).
     * @param structureIdentifier the entry ID
     * @param assemblyIdentifier the assembly ID
     * @param labelSelections selections for each matched residue
     * @param residueTypes mapped residue types
     * @param rootMeanSquareDeviation the alignment score
     * @param transformation the transformation determined by the alignment
     */
    public Hit(String structureIdentifier,
               String assemblyIdentifier,
               List<LabelSelection> labelSelections,
               List<ResidueType> residueTypes,
               RootMeanSquareDeviation rootMeanSquareDeviation,
               Transformation transformation) {
        this.structureIdentifier = structureIdentifier;
        this.assemblyIdentifier = assemblyIdentifier;
        this.labelSelections = labelSelections;
        this.residueTypes = residueTypes;
        this.rootMeanSquareDeviation = rootMeanSquareDeviation;
        this.transformation = transformation;
    }

    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    public String getAssemblyIdentifier() {
        return assemblyIdentifier;
    }

    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
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
