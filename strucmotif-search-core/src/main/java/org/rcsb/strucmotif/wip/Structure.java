package org.rcsb.strucmotif.wip;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Map;
import java.util.stream.IntStream;

public interface Structure {
    String getStructureIdentifier();

    int getAssemblyCount();

    int getTransformationCount();

    int getModelledChainCount();
    int getModelledResidueCount();
    int getModelledAtomCount();

    int getInstancedChainCount();
    int getInstancedResidueCount();
    int getInstancedAtomCount();

    String[] getAssemblyIdentifiers();
    String[] getReferencedChainInstances(String assemblyIdentifier);
    String[] getTransformationIdentifiers();
    float[] getTransformations();

    IntStream modelledResidueIndices();
    IntStream instancedResidueIndices();

    // 'mapping' utils
    int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId);
    int getResidueIndex(LabelSelection labelSelection);

    // data access
    String getAssemblyIdentifier(int residueIndex);
    int getLabelSeqId(int residueIndex);
    ResidueType getResidueType(int residueIndex);
    String getLabelAsymId(int residueIndex);
    String getTransformationIdentifier(int residueIndex);
    // must use float here to accommodate transformations
    Map<LabelAtomId, float[]> manifestResidue(int residueIndex);

    // could sort various properties into sub categories on atoms/residues/chains
}
