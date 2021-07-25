package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.Transformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Structure {
    private final String structureIdentifier;
    private final Map<LabelSelection.SparseLabelSelection, Integer> residueMapping;
    private final int[] residueOffsets;
    private final int residueCount;
    private final int atomCount;
    private final String[] labelAtomId;
    private final short[] x;
    private final short[] y;
    private final short[] z;
    private final ResidueType[] residueTypes;
    private final Map<String, List<String>> assemblies;
    private final Map<String, Transformation> transformations;

    public Structure(String structureIdentifier,
                     Map<LabelSelection.SparseLabelSelection, Integer> residueMapping,
                     int[] residueOffsets,
                     ResidueType[] residueTypes,
                     String[] labelAtomId,
                     short[] x,
                     short[] y,
                     short[] z,
                     Map<String, List<String>> assemblies,
                     Map<String, Transformation> transformations) {
        this.structureIdentifier = structureIdentifier;
        this.residueMapping = residueMapping;
        this.residueOffsets = residueOffsets;
        this.residueTypes = residueTypes;
        this.residueCount = residueMapping.size();
        this.atomCount = labelAtomId.length;
        this.labelAtomId = labelAtomId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.assemblies = assemblies;
        this.transformations = transformations;
    }

    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    public List<LabelSelection.SparseLabelSelection> getSparseLabelSelections() {
        return new ArrayList<>(residueMapping.keySet());
    }

    public int getResidueIndex(String labelAsymId, int labelSeqId) {
        // this must be accessed without struct_oper_id
        return residueMapping.get(new LabelSelection.SparseLabelSelection(labelAsymId, labelSeqId));
    }

    public int getResidueCount() {
        return residueCount;
    }

    public int getAtomCount() {
        return atomCount;
    }

    public ResidueType getResidueType(int residueIndex) {
        return residueTypes[residueIndex];
    }

    public Map<String, List<String>> getAssemblies() {
        return assemblies;
    }

    public Map<String, Transformation> getTransformations() {
        return transformations;
    }

    public Transformation getTransformation(String structOperIdentifier) {
        return transformations.get(structOperIdentifier);
    }

    public Map<String, float[]> manifestResidue(LabelSelection labelSelection) {
        return manifestResidue(getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId()), labelSelection.getStructOperId());
    }

    public List<Map<String, float[]>> manifestResidues(List<LabelSelection> labelSelections) {
        return labelSelections.stream()
                .map(this::manifestResidue)
                .collect(Collectors.toList());
    }

    public Map<String, float[]> manifestResidue(int residueIndex) {
        return manifestResidue(residueIndex, "1");
    }

    public Map<String, float[]> manifestResidue(int residueIndex, String structOperIdentifier) {
        Map<String, float[]> out = new LinkedHashMap<>();
        int offsetStart = residueOffsets[residueIndex];
        int offsetEnd = residueIndex + 1 == residueOffsets.length ? labelAtomId.length : residueOffsets[residueIndex + 1];
        Transformation transformation = transformations.get(structOperIdentifier);

        // happens e.g. for 7a3x, there assembly '1' references opers '2' and '3'
        if (transformation == null) {
            transformation = Transformation.IDENTITY_TRANSFORMATION;
        }

        for (int i = offsetStart; i < offsetEnd; i++) {
            String labelAtomId = this.labelAtomId[i];
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
