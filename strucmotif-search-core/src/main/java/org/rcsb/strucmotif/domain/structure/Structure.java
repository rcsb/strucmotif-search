package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.Transformation;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The structure object wraps categories of a mmCIF file and allows access via some utility methods.
 */
public class Structure {
    private final String structureIdentifier;
    private final Map<String, int[]> chainOffsets;
    private final int[] labelSeqId;
    private final int chainCount;
    private final int residueCount;
    private final int atomCount;
    private final int[] residueOffsets;
    private final byte[] residueTypes;
    private final byte[] labelAtomId;
    private final short[] x;
    private final short[] y;
    private final short[] z;
    private final Map<String, Set<String>> assemblies;
    private final Map<String, Transformation> transformations;

    /**
     * Create a structure view.
     * @param structureIdentifier the identifier
     * @param chainOffsets residue indices where label_asym_id changes
     * @param labelSeqId array of all label_seq_id
     * @param residueOffsets the offset of each residue in the atom_site category
     * @param residueTypes the type of each residue
     * @param labelAtomId the type of each atom
     * @param x the x coords of each atom
     * @param y the y coords of each atom
     * @param z the z coords of each atom
     * @param assemblies all assemblies
     * @param transformations all transformations
     */
    public Structure(String structureIdentifier,
                     Map<String, int[]> chainOffsets,
                     int[] labelSeqId,
                     int[] residueOffsets,
                     byte[] residueTypes,
                     byte[] labelAtomId,
                     short[] x,
                     short[] y,
                     short[] z,
                     Map<String, Set<String>> assemblies,
                     Map<String, Transformation> transformations) {
        this.structureIdentifier = structureIdentifier;
        this.chainOffsets = chainOffsets;
        this.labelSeqId = labelSeqId;
        this.residueOffsets = residueOffsets;
        this.residueTypes = residueTypes;
        this.chainCount = chainOffsets.size();
        this.residueCount = residueOffsets.length;
        this.atomCount = labelAtomId.length;
        this.labelAtomId = labelAtomId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.assemblies = assemblies;
        this.transformations = transformations;
    }

    /**
     * This structure's identifier.
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Access to all unique (label_asym_id, label_seq_id) pairs that exist in this structure. This call is expensive and
     * will traverse all internally registered chains and sequence positions and create a collection of associated
     * LabelSelection instances.<p>
     * Make sure to reuse the result returned by this call. Never call in a loop!
     * @return a sorted list of all LabelSelection instances that exist for this structure
     */
    public List<LabelSelection> getLabelSelections() {
        return IntStream.range(0, residueCount)
                .mapToObj(this::getLabelSelection)
                .collect(Collectors.toList());
    }

    /**
     * Look up the index of a residue.
     * @param labelAsymId the chain-ID of the residue
     * @param labelSeqId the sequence position of the residue
     * @return the index of the residue
     * @throws NoSuchElementException if the chain/residue cannot be found
     */
    public int getResidueIndex(String labelAsymId, int labelSeqId) {
        if (!chainOffsets.containsKey(labelAsymId)) {
            throw new NoSuchElementException("Didn't find chain: " + labelAsymId);
        }

        int[] chainOffset = chainOffsets.get(labelAsymId);
        // on the sub-array binary search works
        int index = Arrays.binarySearch(this.labelSeqId, chainOffset[0], chainOffset[1] + 1, labelSeqId);
        if (index < 0) {
            throw new NoSuchElementException("Didn't find residue with label_seq_id " + labelSeqId + " in chain " + labelAsymId);
        }
        return index;
    }

    public LabelSelection getLabelSelection(int residueIndex) {
        String labelAsymId = null;
        for (Map.Entry<String, int[]> entry : chainOffsets.entrySet()) {
            int[] offsets =  entry.getValue();
            if (residueIndex >= offsets[0] && residueIndex <= offsets[1]) {
                labelAsymId = entry.getKey();
            }
        }

        if (labelAsymId == null) {
            throw new NoSuchElementException("Didn't find chain that contains residue index: " + residueIndex);
        }

        int labelSeqId = this.labelSeqId[residueIndex];
        return new LabelSelection(labelAsymId, null, labelSeqId);
    }

    /**
     * Count of all chains in the source CIF file.
     * @return an int
     */
    public int getChainCount() {
        return chainCount;
    }

    /**
     * Count of all residues in the source CIF file.
     * @return an int
     */
    public int getResidueCount() {
        return residueCount;
    }

    /**
     * Count of all atoms/rows in the source CIF file.
     * @return an int
     */
    public int getAtomCount() {
        return atomCount;
    }

    /**
     * Reports the residue type/amino acid at a certain index.
     * @param residueIndex the index of the residue
     * @return a ResidueType instance
     */
    public ResidueType getResidueType(int residueIndex) {
        return ResidueType.values()[residueTypes[residueIndex]];
    }

    /**
     * Access to assembly information.
     * @return Map of all assemblies [assemblyId, (label_asym_id x struct_oper_id)[]]
     */
    public Map<String, Set<String>> getAssemblies() {
        return assemblies;
    }

    /**
     * Access to all transformations.
     * @return Map of transformations [struct_oper_id, Transformation]
     */
    public Map<String, Transformation> getTransformations() {
        return transformations;
    }

    /**
     * Access to a specific transformation.
     * @param structOperIdentifier the struct_oper_id expression
     * @return a Transformation object
     */
    public Transformation getTransformation(String structOperIdentifier) {
        return transformations.get(structOperIdentifier);
    }

    /**
     * Manifest a residue by its LabelSelection.
     * @param labelSelection the residue identifier
     * @return a residue
     */
    public Map<LabelAtomId, float[]> manifestResidue(LabelSelection labelSelection) {
        return manifestResidue(getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId()), labelSelection.getStructOperId());
    }

    /**
     * Manifest a collection of residues by their LabelSelection.
     * @param labelSelections a collection of residue identifiers
     * @return a collection of residues
     */
    public List<Map<LabelAtomId, float[]>> manifestResidues(List<LabelSelection> labelSelections) {
        return labelSelections.stream()
                .map(this::manifestResidue)
                .collect(Collectors.toList());
    }

    /**
     * Manifest a residue by its index.
     * @param residueIndex the index of the residue
     * @return a residue
     */
    public Map<LabelAtomId, float[]> manifestResidue(int residueIndex) {
        return manifestResidue(residueIndex, "1");
    }

    /**
     * 'Manifests' a residue, i.e. extract all relevant atom_site rows and move coordinates of all atoms to a map. Atoms
     * are identified by their label_comp_id, coordinates given as float[3]. The requested transformation will be
     * applied to the coordinates.
     * @param residueIndex the index of the residue
     * @param structOperIdentifier the operator to apply to these coordinates
     * @return a Map [label_atom_id, [x, y, z]]
     */
    public Map<LabelAtomId, float[]> manifestResidue(int residueIndex, String structOperIdentifier) {
        Map<LabelAtomId, float[]> out = new EnumMap<>(LabelAtomId.class);
        int offsetStart = residueOffsets[residueIndex];
        int offsetEnd = residueIndex + 1 == residueOffsets.length ? labelAtomId.length : residueOffsets[residueIndex + 1];
        Transformation transformation = transformations.get(structOperIdentifier);

        // happens e.g. for 7a3x, there assembly '1' references opers '2' and '3'
        if (transformation == null) {
            transformation = Transformation.IDENTITY_TRANSFORMATION;
        }

        for (int i = offsetStart; i < offsetEnd; i++) {
            LabelAtomId labelAtomId = LabelAtomId.values()[this.labelAtomId[i]];
            // ignore 'non-standard' atoms
            if (labelAtomId == LabelAtomId.UNKNOWN_ATOM) {
                continue;
            }

            float[] v = new float[] {
                    x[i] * 0.1f,
                    y[i] * 0.1f,
                    z[i] * 0.1f
            };
            transformation.transform(v, v);
            out.put(labelAtomId, v);
        }
        return out;
    }
}
