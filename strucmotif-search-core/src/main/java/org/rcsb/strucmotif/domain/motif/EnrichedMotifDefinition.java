package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enriched motif definitions wrap a {@link MotifDefinition} and provide additionally access to the underlying
 * {@link Structure} as well as referenced residues.
 */
public class EnrichedMotifDefinition extends MotifDefinition {
    private final Structure structure;
    private final List<Map<LabelAtomId, float[]>> residues;

    /**
     * Construct a enriched motif.
     * @param motifDefinition original definition
     * @param structure underlying structure
     * @param residues all relevant residues
     */
    public EnrichedMotifDefinition(MotifDefinition motifDefinition, Structure structure, List<Map<LabelAtomId, float[]>> residues) {
        super(motifDefinition.getMotifIdentifier(), motifDefinition.getStructureIdentifier(), motifDefinition.getTitle(), motifDefinition.getDescription(), motifDefinition.getLabelSelections(), motifDefinition.getPositionSpecificExchanges());
        this.structure = structure;
        this.residues = residues;
    }

    /**
     * Access to the structure object.
     * @return a {@link Structure}
     */
    public Structure getStructure() {
        return structure;
    }

    /**
     * All relevant residues.
     * @return a collection of maps from atom names to 3D vectors
     */
    public List<Map<LabelAtomId, float[]>> getResidues() {
        return residues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EnrichedMotifDefinition that = (EnrichedMotifDefinition) o;
        return Objects.equals(structure, that.structure) && Objects.equals(residues, that.residues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structure, residues);
    }
}
