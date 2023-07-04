package org.rcsb.strucmotif.domain.structure;

import java.util.Map;
import java.util.stream.IntStream;

public interface Structure {
    String getStructureIdentifier();

    int getModelledChainCount();
    int getModelledResidueCount();
    int getModelledAtomCount();
    int getInstancedChainCount();
    int getInstancedResidueCount();
    int getInstancedAtomCount();
    IntStream modelledResidueIndices();
    IntStream instancedResidueIndices();

    int getAssemblyCount();
    String[] getAssemblyIdentifiers();
    String[] getReferencedChainInstances(String assemblyIdentifier);
    String[] getTransformationIdentifiers();
    float[] getTransformations();
    float[] getTransformation(String transformationIdentifier);
    int getTransformationCount();

    // 'mapping' utils
    int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId);
    int getResidueIndex(LabelSelection labelSelection);
    LabelSelection getLabelSelection(int residueIndex);

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
