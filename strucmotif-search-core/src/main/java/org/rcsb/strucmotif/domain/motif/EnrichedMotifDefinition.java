package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnrichedMotifDefinition extends MotifDefinition {
    private final Structure structure;
    private final List<Map<LabelAtomId, float[]>> residues;

    public EnrichedMotifDefinition(MotifDefinition motifDefinition, Structure structure, List<Map<LabelAtomId, float[]>> residues) {
        super(motifDefinition.getMotifIdentifier(), motifDefinition.getStructureIdentifier(), motifDefinition.getTitle(), motifDefinition.getDescription(), motifDefinition.getLabelSelections(), motifDefinition.getPositionSpecificExchanges());
        this.structure = structure;
        this.residues = residues;
    }

    public Structure getStructure() {
        return structure;
    }

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

    @Override
    public String toString() {
        return super.toString();
    }
}
