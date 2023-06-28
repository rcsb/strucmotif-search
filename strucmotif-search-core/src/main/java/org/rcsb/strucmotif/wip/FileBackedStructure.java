package org.rcsb.strucmotif.wip;

import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FileBackedStructure implements Structure {
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
    private final int depositedChainCount;
    private final int instancedChainCount;

    private final short[] labelSeqIds;
    private final int[] residueOffsets;
    private final byte[] residueTypes;
    private final int depositedResidueCount;
    private final int instancedResidueCount;

    private final byte[] labelAtomIds;
    private final short[] x;
    private final short[] y;
    private final short[] z;
    private final int depositedAtomCount;
    private final int instancedAtomCount;

    public FileBackedStructure(String structureIdentifier, String[] labelAsymIds, int[] chainOffsets, short[] labelSeqIds, int[] residueOffsets, byte[] residueTypes, byte[] labelAtomIds, short[] x, short[] y, short[] z) {
        this(structureIdentifier, null, null, null, null, null, labelAsymIds, chainOffsets, labelSeqIds, residueOffsets, residueTypes, labelAtomIds, x, y, z);
    }

    public FileBackedStructure(String structureIdentifier,

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
        this.assemblyCount = assemblyIdentifiers != null ? assemblyIdentifiers.length : 1;

        this.transformationIdentifiers = transformationIdentifiers;
        this.transformations = transformations;
        this.transformationCount = transformationIdentifiers != null ? transformationIdentifiers.length : 1;

        this.labelAsymIds = labelAsymIds;
        this.chainOffsets = chainOffsets; // start indices of chains
        this.depositedChainCount = labelAsymIds.length;

        this.labelSeqIds = labelSeqIds;
        this.residueOffsets = residueOffsets; // start indices of residues
        this.residueTypes = residueTypes;
        this.depositedResidueCount = labelSeqIds.length;

        this.labelAtomIds = labelAtomIds;
        this.x = x;
        this.y = y;
        this.z = z;
        this.depositedAtomCount = labelAtomIds.length;

        // compute properties
        if (assemblyCount == 1) {
            // no assemblies
            this.instancedChainCount = depositedChainCount;
            this.instancedResidueCount = depositedResidueCount;
            this.instancedAtomCount = depositedAtomCount;
        } else {
            this.instancedChainCount = assemblyReferences.length / 2;

            Map<String, Integer> residuePerChain = new HashMap<>();
            Map<String, Integer> atomsPerChain = new HashMap<>();
            for (int i = 0; i < chainOffsets.length; i++) {
                String chain = labelAsymIds[i];
                int start = chainOffsets[i];
                int end = (i == chainOffsets.length - 1 ? depositedResidueCount : chainOffsets[i + 1]) - 1;

                residuePerChain.put(chain, end - start);
                atomsPerChain.put(chain, residueOffsets[end] - residueOffsets[start]);
            }

            int residueCount = 0;
            int atomCount = 0;
            for (int i = 0; i < assemblyReferences.length - 1; i = i + 2) {
                String chain = assemblyReferences[i];
                residueCount += residuePerChain.get(chain);
                atomCount += atomsPerChain.get(chain);
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
    public int getDepositedChainCount() {
        return depositedChainCount;
    }

    @Override
    public int getDepositedResidueCount() {
        return depositedResidueCount;
    }

    @Override
    public int getDepositedAtomCount() {
        return depositedAtomCount;
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
    public IntStream residueIndices() {
        return IntStream.range(0, instancedResidueCount);
    }

    @Override
    public IntStream atomIndices() {
        return IntStream.range(0, instancedAtomCount);
    }

    @Override
    public LabelAtomId getLabelAtomId(int atomIndex) {
        return LabelAtomId.values()[labelAtomIds[remapAtomIndex(atomIndex)]];
    }

    @Override
    public int getLabelSeqId(int residueIndex) {
        return labelSeqIds[remapResidueIndex(residueIndex)];
    }

    @Override
    public ResidueType getResidueType(int residueIndex) {
        return ResidueType.values()[residueTypes[remapResidueIndex(residueIndex)]];
    }

    @Override
    public float getX(int atomIndex) {
        return x[remapAtomIndex(atomIndex)];
    }

    @Override
    public float getY(int atomIndex) {
        return y[remapAtomIndex(atomIndex)];
    }

    @Override
    public float getZ(int atomIndex) {
        return z[remapAtomIndex(atomIndex)];
    }

    @Override
    public Map<LabelAtomId, float[]> manifestResidue(int residueIndex) {
        // TODO transformation support

        Map<LabelAtomId, float[]> out = new EnumMap<>(LabelAtomId.class);
        int offsetStart = residueOffsets[residueIndex];
        int offsetEnd = residueIndex + 1 == residueOffsets.length ? labelAtomIds.length : residueOffsets[residueIndex + 1];

        for (int i = offsetStart; i < offsetEnd; i++) {
            LabelAtomId labelAtomId = LabelAtomId.values()[labelAtomIds[i]];
            // ignore 'non-standard' atoms
            if (labelAtomId == LabelAtomId.UNKNOWN_ATOM) {
                continue;
            }

            float[] v = new float[] {
                    x[i] * 0.1f,
                    y[i] * 0.1f,
                    z[i] * 0.1f
            };
            out.put(labelAtomId, v);
        }
        return out;
    }

    @Override
    public int remapResidueIndex(int raw) {
        if (assemblyCount == 1) {
            return raw;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int remapAtomIndex(int raw) {
        if (assemblyCount == 1) {
            return raw;
        }
        throw new UnsupportedOperationException();
    }
}
