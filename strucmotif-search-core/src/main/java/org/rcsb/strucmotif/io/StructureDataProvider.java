package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.IOException;
import java.io.InputStream;

/**
 * Access structure data.
 */
public interface StructureDataProvider {
    /**
     * If `strucmotif.in-memory-strategy` is active: Initialize caching by reading all structures and keeping them in
     * memory for fast access.
     */
    void initializeRenumberedStructureCache() throws IOException;

    /**
     * Read everything from an input stream.
     * @param inputStream the source of data, assumed to be following the mmCIF schema
     * @return the corresponding {@link Structure}
     */
    Structure readFromInputStream(InputStream inputStream);

    /**
     * Read a renumbered structure entirely.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    Structure readRenumbered(String structureIdentifier);

    /**
     * Read a selected range from an original structure.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    Structure readOriginal(String structureIdentifier);

    /**
     * Read some structure entirely.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    Structure readSome(String structureIdentifier);

    /**
     * Write a renumbered structure.
     * @param structureIdentifier the structure identifier to write
     * @param mmCifFile the data source
     */
    void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile);

    /**
     * Drop information on a renumbered structure.
     * @param structureIdentifier the structure identifier to remove
     */
    void deleteRenumbered(String structureIdentifier);

    /**
     * Acquire the input stream of an original structure.
     * @param structureIdentifier the structure identifier to read
     * @return the corresponding input stream
     */
    InputStream getOriginalInputStream(String structureIdentifier);
}
