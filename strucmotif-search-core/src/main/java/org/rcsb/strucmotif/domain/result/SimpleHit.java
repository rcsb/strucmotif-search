package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.AssemblyIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.List;

/**
 * A simple hit without transformation information.
 */
public class SimpleHit implements Hit {
    private final StructureIdentifier structureIdentifier;
    private final AssemblyIdentifier assemblyIdentifier;
    private final List<LabelSelection> selection;
    private final GeometricDescriptorScore geometricDescriptorScore;

    public SimpleHit(StructureIdentifier structureIdentifier, AssemblyIdentifier assemblyIdentifier, List<LabelSelection> selection, GeometricDescriptorScore geometricDescriptorScore) {
        this.structureIdentifier = structureIdentifier;
        this.assemblyIdentifier = assemblyIdentifier;
        this.selection = selection;
        this.geometricDescriptorScore = geometricDescriptorScore;
    }

    @Override
    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    @Override
    public AssemblyIdentifier getAssemblyIdentifier() {
        return assemblyIdentifier;
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
