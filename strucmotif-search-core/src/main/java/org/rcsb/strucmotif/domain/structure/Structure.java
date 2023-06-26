package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.Transformation;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The structure object wraps categories of a mmCIF file and allows access via some utility methods.
 */
public class Structure {
    private final String structureIdentifier;
    private final String[] chainIds;
    private final int[] chainOffsets;
    private final short[] labelSeqId;
    private final int chainCount;
    private final int residueCount;
    private final int atomCount;
    private final int[] residueOffsets;
    private final byte[] residueTypes;
    private final byte[] labelAtomId;
    private final short[] x;
    private final short[] y;
    private final short[] z;
    private final String[] assemblyIds;
    private final String[][] chainsReferencedByAssembly;
    private final String[] transformationIds;
    private final Transformation[] transformations;

    /**
     * Create a structure view.
     * @param structureIdentifier the identifier
     * @param chainIds registered chains
     * @param chainOffsets residue indices where label_asym_id changes
     * @param labelSeqId array of all label_seq_id
     * @param residueOffsets the offset of each residue in the atom_site category
     * @param residueTypes the type of each residue
     * @param labelAtomId the type of each atom
     * @param x the x coords of each atom
     * @param y the y coords of each atom
     * @param z the z coords of each atom
     * @param assemblyIds all registered assemblies
     * @param chainsReferencedByAssembly all chains that are associated to a certain assembly
     * @param transformationIds all registered transformations
     * @param transformations all transformations
     */
    public Structure(String structureIdentifier,
                     String[] chainIds,
                     int[] chainOffsets,
                     short[] labelSeqId,
                     int[] residueOffsets,
                     byte[] residueTypes,
                     byte[] labelAtomId,
                     short[] x,
                     short[] y,
                     short[] z,
                     String[] assemblyIds,
                     String[][] chainsReferencedByAssembly,
                     String[] transformationIds,
                     Transformation[] transformations) {
        this.structureIdentifier = structureIdentifier;
        this.chainIds = chainIds;
        this.chainOffsets = chainOffsets; // note, this array is actually 1 pos longer than the chainIds
        this.labelSeqId = labelSeqId;
        this.residueOffsets = residueOffsets;
        this.residueTypes = residueTypes;
        this.chainCount = chainIds.length;
        this.residueCount = residueOffsets.length;
        this.atomCount = labelAtomId.length;
        this.labelAtomId = labelAtomId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.assemblyIds = assemblyIds;
        this.chainsReferencedByAssembly = chainsReferencedByAssembly;
        this.transformationIds = transformationIds;
        this.transformations = transformations;
    }

    private int indexOf(String[] values, String v) {
        Objects.requireNonNull(v);
        for (int i = 0; i < values.length; i++) {
            if (v.equals(values[i])) {
                return i;
            }
        }
        return -1;
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
        int chainIndex = indexOf(chainIds, labelAsymId);
        if (chainIndex == -1) {
            throw new NoSuchElementException("Didn't find chain: " + labelAsymId);
        }

        int chainStart = chainOffsets[chainIndex];
        int chainEnd = chainOffsets[chainIndex + 1];
        // on the sub-array binary search works
        int index = Arrays.binarySearch(this.labelSeqId, chainStart, chainEnd, (short) labelSeqId);
        if (index < 0) {
            throw new NoSuchElementException("Didn't find residue with label_seq_id " + labelSeqId + " in chain " + labelAsymId);
        }
        return index;
    }

    /**
     * Retrieve the {@link LabelSelection} of a single residue, identified by its index.
     * @param residueIndex the position
     * @return a {@link LabelSelection}
     */
    public LabelSelection getLabelSelection(int residueIndex) {
        int chainIndex = -1;
        for (int i = 0; i < chainIds.length; i++) {
            if (residueIndex >= chainOffsets[i] && residueIndex <= chainOffsets[i + 1]) {
                chainIndex = i;
            }
        }

        if (chainIndex == -1) {
            throw new NoSuchElementException("Didn't find chain in '" + structureIdentifier + "' that contains residue index: " +
                    residueIndex + " - Chain offsets: " + Arrays.toString(chainIds) + " -> " + Arrays.toString(chainOffsets) + "\n");
        }

        String labelAsymId = chainIds[chainIndex];
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
    public Map<String, String[]> getAssemblies() {
        Map<String, String[]> assemblies  = new LinkedHashMap<>();
        for (int i = 0; i < assemblyIds.length; i++) {
            assemblies.put(assemblyIds[i], this.chainsReferencedByAssembly[i]);
        }
        return assemblies;
    }

    /**
     * Access to all transformations.
     * @return Map of transformations [struct_oper_id, Transformation]
     */
    public Map<String, Transformation> getTransformations() {
        Map<String, Transformation> transformations = new LinkedHashMap<>();
        for (int i = 0; i < transformationIds.length; i++) {
            transformations.put(transformationIds[i], this.transformations[i]);
        }
        return transformations;
    }

    /**
     * Access to a specific transformation by its struct_oper_id String.
     * @param structOperIdentifier the struct_oper_id expression
     * @return a Transformation object
     */
    public Transformation getTransformation(String structOperIdentifier) {
        int transformationIndex = indexOf(transformationIds, structOperIdentifier);
        return transformations[transformationIndex];
    }

    /**
     * Access to a specific transformation.
     * @param transformationIndex the internal index
     * @return a Transformation object
     */
    public Transformation getTransformation(int transformationIndex) {
        return transformations[transformationIndex];
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
        return manifestResidue(residueIndex, Transformation.DEFAULT_OPERATOR);
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
        int transformationIndex = indexOf(transformationIds, structOperIdentifier);
        Transformation transformation;

        // happens e.g. for 7a3x, there assembly '1' references opers '2' and '3'
        if (transformationIndex == -1) {
            transformation = Transformation.IDENTITY_TRANSFORMATION;
        } else {
            transformation = transformations[transformationIndex];
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
