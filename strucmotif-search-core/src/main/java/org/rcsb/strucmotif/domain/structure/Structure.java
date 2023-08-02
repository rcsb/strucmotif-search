package org.rcsb.strucmotif.domain.structure;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * A structure describes an entry and navigates several flat data arrays. Access to information is residue-centric.
 */
public interface Structure {
    /**
     * A unique key.
     * @return a String
     */
    String getStructureIdentifier();

    /**
     * Number of chains in atom_site.
     * @return an int
     */
    int getModelledChainCount();

    /**
     * Number of residues in atom_site.
     * @return an int
     */
    int getModelledResidueCount();

    /**
     * Number of atoms in atom_site.
     * @return an int
     */
    int getModelledAtomCount();

    /**
     * Number of chains (including copies from assembly gen).
     * @return an int
     */
    int getInstancedChainCount();

    /**
     * Number of residues (including copies from assembly gen).
     * @return an int
     */
    int getInstancedResidueCount();

    /**
     * Number of atoms (including copies from assembly gen).
     * @return an int
     */
    int getInstancedAtomCount();

    /**
     * Traverse all modelled (i.e., the asymmetric unit) residues.
     * @return an IntStream of all indices from atom_site
     */
    IntStream modelledResidueIndices();

    /**
     * Traverse all instanced (i.e., assembly-aware) residues.
     * @return an IntStream of all indices, including copies from assembly gen
     */
    IntStream instancedResidueIndices();

    /**
     * Number of assemblies.
     * @return an int
     */
    int getAssemblyCount();

    /**
     * Access to all assembly keys.
     * @return flat array of all assembly identifiers
     */
    String[] getAssemblyIdentifiers();

    /**
     * All chains with the corresponding transformation that are associated with an assembly.
     * @param assemblyIdentifier assembly key
     * @return flat array with tuples of (label_asym_id, struct_oper_id)
     */
    String[] getReferencedChainInstances(String assemblyIdentifier);

    /**
     * Access to all transformation keys.
     * @return flat array of all transformation identifiers
     */
    String[] getTransformationIdentifiers();

    /**
     * Access to all transformations.
     * @return flat array of all transformation data in blocks of 16
     */
    float[] getTransformations();

    /**
     * Access a specific transformation.
     * @param transformationIdentifier the key
     * @return a 16-value float array that contains this transform
     */
    float[] getTransformation(String transformationIdentifier);

    /**
     * Reports the number of transformations.
     * @return an int
     */
    int getTransformationCount();

    // 'mapping' utils

    /**
     * Map "public" residue information to an internal residue index. If multiple indices match these properties, then
     * only the 1st one is returned.
     * @param labelAsymId chain id
     * @param structOperId transformation id
     * @param labelSeqId sequence position
     * @return an int
     */
    int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId);

    /**
     * Map "public" residue information to an internal residue index.
     * @param assemblyIdentifier assembly identifier
     * @param labelAsymId chain id
     * @param structOperId transformation id
     * @param labelSeqId sequence position
     * @return an int
     */
    int getResidueIndex(String assemblyIdentifier, String labelAsymId, String structOperId, int labelSeqId);

    /**
     * Map "public" residue information to an internal residue index. If multiple indices match these properties, then
     * only the 1st one is returned.
     * @param labelSelection tuple of (label_asym_id, struct_oper_id, label_seq_id)
     * @return an int
     */
    int getResidueIndex(LabelSelection labelSelection);

    /**
     * Map "public" residue information to an internal residue index. This function is assembly-aware and won't just
     * return the 1st index matching these properties.
     * @param assemblyIdentifier assembly identifier
     * @param labelSelection tuple of (label_asym_id, struct_oper_id, label_seq_id)
     * @return an int
     */
    int getResidueIndex(String assemblyIdentifier, LabelSelection labelSelection);

    /**
     * Map an internal residue index to "public" residue information.
     * @param residueIndex an int
     * @return tuple of (label_asym_id, struct_oper_id, label_seq_id)
     */
    LabelSelection getLabelSelection(int residueIndex);

    // data access

    /**
     * Access the assembly identifier of a residue.
     * @param residueIndex residue address
     * @return its assembly identifier
     */
    String getAssemblyIdentifier(int residueIndex);

    /**
     * Access the label_seq_id of a residue.
     * @param residueIndex residue address
     * @return its label_seq_id
     */
    int getLabelSeqId(int residueIndex);

    /**
     * Access the three-letter-code of a residue.
     * @param residueIndex residue address
     * @return its label_comp_id (but as ResidueType instance)
     */
    ResidueType getResidueType(int residueIndex);

    /**
     * Access chain identifier of a residue.
     * @param residueIndex residue address
     * @return its label_asym_id
     */
    String getLabelAsymId(int residueIndex);

    /**
     * Access the transformation identifier of a residue.
     * @param residueIndex residue address
     * @return its transformation identifier
     */
    String getTransformationIdentifier(int residueIndex);

    /**
     * Access coordinates of a residue in usable form.
     * @param residueIndex residue address
     * @return a map with label_atom_id as key and its position as Vec3
     */
    Map<LabelAtomId, float[]> manifestResidue(int residueIndex); // must use float here to accommodate transformations
}
