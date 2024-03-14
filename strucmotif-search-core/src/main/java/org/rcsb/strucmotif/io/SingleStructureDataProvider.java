package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * An implementation of a {@link StructureDataProvider} that only deals with a single structure. Used in the
 * 'detect-motif' mode (see {@link org.rcsb.strucmotif.domain.query.MotifContextBuilder}).
 */
public class SingleStructureDataProvider implements StructureDataProvider {
    private final Structure structure;

    /**
     * Create a data provider for this structure.
     * @param structure the content
     */
    public SingleStructureDataProvider(Structure structure) {
        this.structure = structure;
    }

    @Override
    public void initializeRenumberedStructureCache() {
        // no need to do anything
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        return structure;
    }

    @Override
    public Structure readRenumbered(String structureIdentifier) {
        return structure;
    }

    @Override
    public Structure readOriginal(String structureIdentifier) {
        return structure;
    }

    @Override
    public Structure readSome(String structureIdentifier) {
        return structure;
    }

    @Override
    public void enterWriteMode() {
        immutable();
    }

    @Override
    public void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile) {
        immutable();
    }

    @Override
    public void deleteRenumbered(Collection<String> structureIdentifiers) {
        immutable();
    }

    @Override
    public InputStream getOriginalInputStream(String structureIdentifier) {
        return null;
    }

    @Override
    public void commit() {
        immutable();
    }

    private void immutable() {
        throw new UnsupportedOperationException("This data provider is read-only");
    }

    @Override
    public Set<String> reportKnownFiles() {
        throw new UnsupportedOperationException("This data provider contains a single not registered structure");
    }
}
