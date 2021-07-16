package org.rcsb.strucmotif.domain.identifier;

public class GlobalResidueIdentifier {
    private final StructureIdentifier structureIdentifier;
    private final String labelAsymId;
    private final String structOperId;
    private final int labelSeqId;

    public GlobalResidueIdentifier(StructureIdentifier structureIdentifier, String labelAsymId, String structOperId, int labelSeqId) {
        this.structureIdentifier = structureIdentifier;
        this.labelAsymId = labelAsymId;
        this.structOperId = structOperId;
        this.labelSeqId = labelSeqId;
    }

    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    public String getLabelAsymId() {
        return labelAsymId;
    }

    public String getStructOperId() {
        return structOperId;
    }

    public int getLabelSeqId() {
        return labelSeqId;
    }
}
