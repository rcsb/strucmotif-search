package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.List;

public class SimpleHit implements Hit {
    private final StructureIdentifier structureIdentifier;
    private final List<LabelSelection> selection;
    private final GeometricDescriptorScore geometricDescriptorScore;

    public SimpleHit(StructureIdentifier structureIdentifier, List<LabelSelection> selection, GeometricDescriptorScore geometricDescriptorScore) {
        this.structureIdentifier = structureIdentifier;
        this.selection = selection;
        this.geometricDescriptorScore = geometricDescriptorScore;
    }

    @Override
    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    @Override
    public List<LabelSelection> getSelection() {
        return selection;
    }

    @Override
    public GeometricDescriptorScore getGeometricDescriptorScore() {
        return geometricDescriptorScore;
    }
}
