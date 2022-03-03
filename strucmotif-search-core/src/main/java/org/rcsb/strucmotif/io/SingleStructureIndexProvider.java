package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.query.StructureDeterminationMethodology;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.Set;

public class SingleStructureIndexProvider implements StructureIndexProvider {
    private final int structureIndex;
    private final String structureIdentifier;

    public SingleStructureIndexProvider(Structure structure) {
        this.structureIndex = -1;
        this.structureIdentifier = structure.getStructureIdentifier();
    }

    @Override
    public String selectStructureIdentifier(int structureIndex) {
        return structureIdentifier;
    }

    @Override
    public int selectStructureIndex(String structureIdentifier) {
        return structureIndex;
    }

    @Override
    public int selectOrMintStructureIndex(String structureIdentifier) {
        throw new UnsupportedOperationException("This index provider is read-only");
    }

    @Override
    public int nextStructureIndex() {
        throw new UnsupportedOperationException("This index provider is read-only");
    }

    @Override
    public boolean containsKey(String structureIdentifier) {
        return this.structureIdentifier.equals(structureIdentifier);
    }

    @Override
    public boolean containsKey(int structureIndex) {
        return this.structureIndex == structureIndex;
    }

    @Override
    public Set<Integer> selectBySearchSpace(StructureDeterminationMethodology structureDeterminationMethodology) {
        return Set.of(structureIndex);
    }
}
