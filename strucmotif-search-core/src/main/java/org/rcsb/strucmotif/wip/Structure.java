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
    IntStream modelledAtomIndices();
    IntStream instancedAtomIndices();

    // 'mapping' utils
    int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId);
    int getResidueIndex(LabelSelection labelSelection);

    // data access
    String getAssemblyIdentifier(int residueIndex);
    LabelAtomId getLabelAtomId(int atomIndex); // TODO transform support
    int getLabelSeqId(int residueIndex);
    ResidueType getResidueType(int residueIndex);
    String getLabelAsymId(int residueIndex);
    String getTransformationIdentifier(int residueIndex);
    // must use float here to accommodate transformations
    float getX(int atomIndex); // TODO transform support
    float getY(int atomIndex); // TODO transform support
    float getZ(int atomIndex); // TODO transform support
    Map<LabelAtomId, float[]> manifestResidue(int residueIndex); // TODO transform support

    // could sort various properties into sub categories on atoms/residues/chains
}
