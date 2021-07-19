package org.rcsb.strucmotif2.domain.structure;

import org.rcsb.strucmotif2.domain.Transformation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SparseStructure {
    private final Map<String, Object[]> data;
    private final Map<String, Transformation> transformations;

    public SparseStructure(Map<String, Object[]> data, Map<String, Transformation> transformations) {
        this.data = data;
        this.transformations = transformations;
    }

    public Transformation getTransformation(String structOperId) {
        return transformations.getOrDefault(structOperId, Transformation.IDENTITY_TRANSFORMATION);
    }

    public Object[] getResidue(String labelAsymId, int labelSeqId) {
        return data.get(labelAsymId + ":" + labelSeqId);
    }

    public static SparseStructure load(InputStream inputStream) {
        Map<String, Object[]> data = new HashMap<>();
        Map<String, Transformation> transformations = new HashMap<>();
        return new SparseStructure(data, transformations);
    }
}
