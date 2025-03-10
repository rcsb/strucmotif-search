package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.io.DefaultStructureReader;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Structure as parsed from a CIF file.
 */
public class DefaultStructure implements Structure {
    private static final int[] DEFAULT_OFFSET = new int[] { 0 };
    private final String structureIdentifier;

    private final String[] assemblyIdentifiers;
    private final int[] assemblyOffsets;
    private final String[] assemblyReferences;
    private final int assemblyCount;

    private final String[] transformationIdentifiers;
    private final float[] transformations;
    private final int transformationCount;

    private final String[] labelAsymIds;
    private final int[] chainOffsets;
    private final int[] instancedChainOffsets; // chain/transformation boundaries of residue indices
    private final int[] instancedChainToAssemblyIndices;
    private final int[] instancedChainToLabelAsymIdsIndices;
    private final int[] instancedChainToTransformationIndices;
    private final int modelledChainCount;
    private final int instancedChainCount; // this allows duplications

    private final short[] labelSeqIds;
    private final int[] residueOffsets;
    private final byte[] residueTypes;
    private final int modelledResidueCount;
    private final int instancedResidueCount;

    private final byte[] labelAtomIds;
    private final short[] x;
    private final short[] y;
    private final short[] z;
    private final int modelledAtomCount;
    private final int instancedAtomCount;

    /**
     * Construct a structure using a bunch of arrays composed by a parser.
     * @param structureIdentifier the structure id
     * @param assemblyIdentifiers assembly identifiers
     * @param assemblyOffsets reference positions where assemblies start
     * @param assemblyReferences tuples of (chain name, transform name)
     * @param transformationIdentifiers transform names
     * @param transformations transforms
     * @param labelAsymIds chain identifiers
     * @param chainOffsets residue positions where chains start
     * @param labelSeqIds sequence positions
     * @param residueOffsets atom positions where residues start
     * @param residueTypes per-residue components
     * @param labelAtomIds atom names
     * @param x coords
     * @param y coords
     * @param z coords
     */
    public DefaultStructure(String structureIdentifier,

                            // assembly-level data
                            // all may be null if there's implicitly only assembly "1" and no transformations
                            String[] assemblyIdentifiers,
                            int[] assemblyOffsets, // start positions of an assembly in assemblyReferences
                            String[] assemblyReferences, // tuples of (label_asym_id, transformationIdentifier)
                            String[] transformationIdentifiers, // resolved identifier names like "Px61"
                            float[] transformations, // blocks of 16 values in row-major indexing

                            // chain-level data
                            String[] labelAsymIds,
                            int[] chainOffsets,

                            // residue-level data
                            short[] labelSeqIds,
                            int[] residueOffsets,
                            byte[] residueTypes,

                            // atom-level data
                            byte[] labelAtomIds,
                            short[] x,
                            short[] y,
                            short[] z) {
        this.structureIdentifier = structureIdentifier;

        this.assemblyIdentifiers = assemblyIdentifiers;
        this.assemblyOffsets = assemblyOffsets;
        this.assemblyReferences = assemblyReferences;
        this.assemblyCount = assemblyIdentifiers.length;

        this.transformationIdentifiers = transformationIdentifiers;
        this.transformations = transformations;
        this.transformationCount = transformationIdentifiers.length;

        this.labelAsymIds = labelAsymIds;
        this.chainOffsets = chainOffsets; // start indices of chains
        this.modelledChainCount = labelAsymIds.length;

        this.labelSeqIds = labelSeqIds;
        this.residueOffsets = residueOffsets; // start indices of residues
        this.residueTypes = residueTypes;
        this.modelledResidueCount = labelSeqIds.length;

        this.labelAtomIds = labelAtomIds;
        this.x = x;
        this.y = y;
        this.z = z;
        this.modelledAtomCount = labelAtomIds.length;

        // compute properties
        if (assemblyCount == 1 && transformationCount == 1) {
            // no transformations
            this.instancedChainCount = modelledChainCount;
            this.instancedResidueCount = modelledResidueCount;
            this.instancedAtomCount = modelledAtomCount;

            this.instancedChainOffsets = chainOffsets;
            this.instancedChainToAssemblyIndices = chainOffsets.length == 1 ? DEFAULT_OFFSET : new int[chainOffsets.length];
            this.instancedChainToLabelAsymIdsIndices = chainOffsets.length == 1 ? DEFAULT_OFFSET : IntStream.range(0, chainOffsets.length).toArray();
            this.instancedChainToTransformationIndices = instancedChainToAssemblyIndices;
        } else {
            this.instancedChainCount = assemblyReferences.length / 2;
            this.instancedChainOffsets = new int[instancedChainCount];
            this.instancedChainToAssemblyIndices = new int[instancedChainCount];
            this.instancedChainToLabelAsymIdsIndices = new int[instancedChainCount];
            this.instancedChainToTransformationIndices = new int[instancedChainCount];

            Map<String, Integer> residuePerChain = new HashMap<>();
            Map<String, Integer> atomsPerChain = new HashMap<>();
            for (int i = 0; i < chainOffsets.length; i++) {
                String chain = labelAsymIds[i];
                int start = chainOffsets[i];
                int endExclusive = (i == chainOffsets.length - 1 ? modelledResidueCount : chainOffsets[i + 1]);

                residuePerChain.put(chain, endExclusive - start);
                atomsPerChain.put(chain, (endExclusive == residueOffsets.length ? modelledAtomCount : residueOffsets[endExclusive]) - residueOffsets[start]);
            }

            int residueCount = 0;
            int atomCount = 0;
            for (int i = 0; i < assemblyReferences.length - 1; i = i + 2) {
                String chain = assemblyReferences[i];
                String transformationIdentifier = assemblyReferences[i + 1];

                instancedChainOffsets[i / 2] = residueCount;
                residueCount += residuePerChain.get(chain);
                atomCount += atomsPerChain.get(chain);

                instancedChainToAssemblyIndices[i / 2] = offsetArrayIndexOf(assemblyOffsets, i);
                instancedChainToLabelAsymIdsIndices[i / 2] = indexOf(labelAsymIds, chain);
                instancedChainToTransformationIndices[i / 2] = indexOf(transformationIdentifiers, transformationIdentifier);
            }

            this.instancedResidueCount = residueCount;
            this.instancedAtomCount = atomCount;
        }
    }

    @Override
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    @Override
    public int getAssemblyCount() {
        return assemblyCount;
    }

    @Override
    public int getTransformationCount() {
        return transformationCount;
    }

    @Override
    public int getModelledChainCount() {
        return modelledChainCount;
    }

    @Override
    public int getModelledResidueCount() {
        return modelledResidueCount;
    }

    @Override
    public int getModelledAtomCount() {
        return modelledAtomCount;
    }

    @Override
    public int getInstancedChainCount() {
        return instancedChainCount;
    }

    @Override
    public int getInstancedResidueCount() {
        return instancedResidueCount;
    }

    @Override
    public int getInstancedAtomCount() {
        return instancedAtomCount;
    }

    @Override
    public IntStream instancedResidueIndices() {
        return IntStream.range(0, instancedResidueCount);
    }

    @Override
    public IntStream modelledResidueIndices() {
        return IntStream.range(0, modelledResidueCount);
    }

    @Override
    public String getAssemblyIdentifier(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        return assemblyIdentifiers[instancedChainToAssemblyIndices[instancedChainIndex]];
    }

    @Override
    public String getLabelAsymId(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        return labelAsymIds[instancedChainToLabelAsymIdsIndices[instancedChainIndex]];
    }

    @Override
    public String getTransformationIdentifier(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        return transformationIdentifiers[instancedChainToTransformationIndices[instancedChainIndex]];
    }

    @Override
    public float[] getTransformation(String transformationIdentifier) {
        int transformationIndex = indexOf(transformationIdentifiers, transformationIdentifier);
        if (transformationIndex < 0) {
            throw new NoSuchElementException("No transformation with name '" + transformationIdentifier + "' registered");
        }
        float[] out = new float[16];
        System.arraycopy(transformations, transformationIndex * 16, out, 0, out.length);
        return out;
    }

    @Override
    public int getLabelSeqId(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        return labelSeqIds[residueIndex - instancedChainOffsets[instancedChainIndex] + chainOffsets[instancedChainToLabelAsymIdsIndices[instancedChainIndex]]];
    }

    @Override
    public ResidueType getResidueType(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        return ResidueType.values[residueTypes[residueIndex - instancedChainOffsets[instancedChainIndex] + chainOffsets[instancedChainToLabelAsymIdsIndices[instancedChainIndex]]]];
    }

    @Override
    public Map<LabelAtomId, float[]> manifestResidue(int residueIndex) {
        Map<LabelAtomId, float[]> out = new EnumMap<>(LabelAtomId.class);
        if (assemblyCount == 1 && transformationCount == 1) {
            int offsetStart = residueOffsets[residueIndex];
            int offsetEnd = residueIndex + 1 == residueOffsets.length ? labelAtomIds.length : residueOffsets[residueIndex + 1];
            for (int j = offsetStart; j < offsetEnd; j++) {
                LabelAtomId labelAtomId = LabelAtomId.values[labelAtomIds[j]];
                // ignore 'non-standard' atoms and non-first occurrence of names
                if (labelAtomId == LabelAtomId.UNKNOWN_ATOM || out.containsKey(labelAtomId)) {
                    continue;
                }

                float[] v = new float[] { x[j] * 0.1f, y[j] * 0.1f, z[j] * 0.1f };
                out.put(labelAtomId, v);
            }
            return out;
        }

        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        int modelledResidueIndex = residueIndex - instancedChainOffsets[instancedChainIndex] + chainOffsets[instancedChainToLabelAsymIdsIndices[instancedChainIndex]];
        int transformIndex = indexOf(transformationIdentifiers, assemblyReferences[2 * instancedChainIndex + 1]);

        int offsetStart = residueOffsets[modelledResidueIndex];
        int offsetEnd = modelledResidueIndex + 1 == residueOffsets.length ? labelAtomIds.length : residueOffsets[modelledResidueIndex + 1];

        for (int j = offsetStart; j < offsetEnd; j++) {
            LabelAtomId labelAtomId = LabelAtomId.values[labelAtomIds[j]];
            // ignore 'non-standard' atoms and non-first occurrence of names
            if (labelAtomId == LabelAtomId.UNKNOWN_ATOM || out.containsKey(labelAtomId)) {
                continue;
            }

            float[] v = new float[] { x[j] * 0.1f, y[j] * 0.1f, z[j] * 0.1f };
            transform(v, transformations, transformIndex * 16);
            out.put(labelAtomId, v);
        }
        return out;
    }

    @Override
    public String[] getAssemblyIdentifiers() {
        return assemblyIdentifiers;
    }

    @Override
    public String[] getReferencedChainInstances(String assemblyIdentifier) {
        int assemblyIndex = indexOf(assemblyIdentifiers, assemblyIdentifier);
        int start = assemblyOffsets[assemblyIndex];
        int end = assemblyIndex + 1 == assemblyOffsets.length ? assemblyReferences.length : assemblyOffsets[assemblyIndex + 1];
        String[] out = new String[end - start];
        System.arraycopy(assemblyReferences, start, out, 0, out.length);
        return out;
    }

    @Override
    public String[] getTransformationIdentifiers() {
        return transformationIdentifiers;
    }

    @Override
    public float[] getTransformations() {
        return transformations;
    }

    @Override
    public int getResidueIndex(String labelAsymId, String structOperId, int labelSeqId) {
        int instancedChainIndex = assemblyReferenceIndexOf(assemblyReferences, labelAsymId, structOperId, 0, assemblyReferences.length);
        return getResidueIndex(labelAsymId, labelSeqId, instancedChainIndex);
    }

    @Override
    public int getResidueIndex(String assemblyIdentifier, String labelAsymId, String structOperId, int labelSeqId) {
        int assemblyIndex = indexOf(assemblyIdentifiers, assemblyIdentifier);
        if (assemblyIndex < 0) {
            throw new NoSuchElementException("Didn't find assembly '" + assemblyIdentifier + "'");
        }
        int assemblyEnd = assemblyIndex == assemblyOffsets.length - 1 ? assemblyReferences.length : assemblyOffsets[assemblyIndex + 1];
        int instancedChainIndex = assemblyReferenceIndexOf(assemblyReferences, labelAsymId, structOperId, assemblyOffsets[assemblyIndex], assemblyEnd);
        return getResidueIndex(labelAsymId, labelSeqId, instancedChainIndex);
    }

    private int getResidueIndex(String labelAsymId, int labelSeqId, int instancedChainIndex) {
        int chainIndex = instancedChainToLabelAsymIdsIndices[instancedChainIndex];
        int chainEnd = chainIndex == chainOffsets.length - 1 ? labelSeqIds.length : chainOffsets[chainIndex + 1];
        int residueIndex = Arrays.binarySearch(labelSeqIds, chainOffsets[chainIndex], chainEnd, (short) labelSeqId);
        if (residueIndex < 0) {
            throw new NoSuchElementException("Didn't find residue with label_seq_id '" + labelSeqId + "' in chain '" + labelAsymId + "'");
        }
        return residueIndex + instancedChainOffsets[instancedChainIndex] - chainOffsets[chainIndex];
    }

    @Override
    public int getResidueIndex(LabelSelection labelSelection) {
        return getResidueIndex(labelSelection.labelAsymId(), labelSelection.structOperId(), labelSelection.labelSeqId());
    }

    @Override
    public int getResidueIndex(String assemblyIdentifier, LabelSelection labelSelection) {
        return getResidueIndex(assemblyIdentifier, labelSelection.labelAsymId(), labelSelection.structOperId(), labelSelection.labelSeqId());
    }

    @Override
    public LabelSelection getLabelSelection(int residueIndex) {
        int instancedChainIndex = offsetArrayIndexOf(instancedChainOffsets, residueIndex);
        int labelAsymIdIndex = instancedChainToLabelAsymIdsIndices[instancedChainIndex];
        String labelAsymId = labelAsymIds[labelAsymIdIndex];
        String transformationIdentifier = transformationIdentifiers[instancedChainToTransformationIndices[instancedChainIndex]];
        int labelSeqId = labelSeqIds[residueIndex - instancedChainOffsets[instancedChainIndex] + chainOffsets[labelAsymIdIndex]];
        return new LabelSelection(labelAsymId, transformationIdentifier, labelSeqId);
    }

    private static int assemblyReferenceIndexOf(String[] assemblyReferences, String labelAsymId, String structOperId, int start, int end) {
        int chainIndex = -1;
        for (int i = start; i < end - 1; i = i + 2) {
            if (assemblyReferences[i].equals(labelAsymId) && assemblyReferences[i + 1].equals(structOperId)) {
                chainIndex = i / 2;
                break;
            }
        }
        if (chainIndex < 0) {
            throw new NoSuchElementException("No chain '" + labelAsymId + "' with transform '" + structOperId + "'");
        }
        return chainIndex;
    }

    /**
     * Use when data isn't necessarily sorted, such as chain identifiers.
     * @param data array to search in
     * @param key element to find
     * @return index of the element, -1 if not present
     */
    private static int indexOf(String[] data, String key) {
        for (int i = 0; i < data.length; i++) {
            if (data[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Use on sorted arrays of offsets to find the index that contains information on this element.
     * @param data sparse array of offsets to search in
     * @param key element to find
     * @return index of the offset that is the largest value below the key
     */
    static int offsetArrayIndexOf(int[] data, int key) {
        // unsafe if elements past the last offset are requested
        return binarySearch(data, 0, data.length, key);
    }

    /**
     * Returns the index of the next smaller element if not found.
     */
    private static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid];

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return low - 1;  // key not found
    }

    private static void transform(float[] v, float[] m, int offset) {
        // nop on identity matrices
        if (m == DefaultStructureReader.IDENTITY_TRANSFORM) {
            return;
        }
        if (m[offset] == 1.0f && m[offset + 5] == 1.0f && m[offset + 10] == 1.0f) {
            return;
        }

        float x = v[0];
        float y = v[1];
        float z = v[2];

        v[0] = m[offset] * x + m[offset + 1] * y + m[offset + 2] * z + m[offset + 3];
        v[1] = m[offset + 4] * x + m[offset + 5] * y + m[offset + 6] * z + m[offset + 7];
        v[2] = m[offset + 8] * x + m[offset + 9] * y + m[offset + 10] * z + m[offset + 11];
    }
}
