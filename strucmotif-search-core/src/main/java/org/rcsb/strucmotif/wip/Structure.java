package org.rcsb.strucmotif.wip;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Map;
import java.util.stream.IntStream;

public interface Structure {
    String getStructureIdentifier();

    int getDepositedChainCount();
    int getDepositedResidueCount();
    int getDepositedAtomCount();

    int getInstancedChainCount();
    int getInstancedResidueCount();
    int getInstancedAtomCount();

    IntStream residueIndices(); // traverse all residues (deposited and instanced)
    IntStream atomIndices(); // traverse all atoms (deposited and instanced)
//    String[] getAssemblyIdentifiers();
//    String[] getReferencedChainInstances(String assemblyIdentifier);
//    String[] getTransformationIdentifiers();
//    Transformation[] getTransformations();

    // 'mapping' utils
    // access to all residues?
    // int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId);
    // int getResidueIndex(LabelSelection labelSelection);
    // LabelSelection getLabelSelection(int residueIndex);

    // data access
     LabelAtomId getLabelAtomId(int atomIndex);
     int getLabelSeqId(int residueIndex);
     ResidueType getResidueType(int residueIndex);
     // must use float here to accommodate transformations
     float getX(int atomIndex);
     float getY(int atomIndex);
     float getZ(int atomIndex);
     Map<LabelAtomId, float[]> manifestResidue(int residueIndex);

    int remapResidueIndex(int raw);

    int remapAtomIndex(int raw);

    // could sort various properties into sub categories on atoms/residues/chains
}
