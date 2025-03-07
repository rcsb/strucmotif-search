package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Access structure data.
 */
public interface StructureDataProvider extends Committable {
    int DEFAULT_MODEL_IDENTIFIER = 1;

    /**
     * If `strucmotif.in-memory-strategy` is active: Initialize caching by reading all structures and keeping them in
     * memory for fast access.
     */
    void initializeRenumberedStructureCache();

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
     * Must be called before invoking {@link #writeRenumbered(String, MmCifFile, int)}.
     * @throws IOException if files can't be created
     */
    void enterWriteMode() throws IOException;

    /**
     * Write a renumbered structure. Make sure to call {@link #enterWriteMode()} if you need to write structure data.
     * @param structureIdentifier the structure identifier to write
     * @param mmCifFile the data source
     */
    default void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile) {
        writeRenumbered(structureIdentifier, mmCifFile, DEFAULT_MODEL_IDENTIFIER);
    }

    /**
     * Write a renumbered structure. Make sure to call {@link #enterWriteMode()} if you need to write structure data.
     * @param structureIdentifier the structure identifier to write
     * @param mmCifFile the data source
     * @param modelIdentifier specific model to extract
     */
    void writeRenumbered(String structureIdentifier, MmCifFile mmCifFile, int modelIdentifier);

    /**
     * Drop information on a renumbered structure.
     * @param structureIdentifiers the collection of structure identifier to remove
     */
    void deleteRenumbered(Collection<String> structureIdentifiers);

    /**
     * Acquire the input stream of an original structure.
     * @param structureIdentifier the structure identifier to read
     * @return the corresponding input stream
     */
    InputStream getOriginalInputStream(String structureIdentifier);

    /**
     * Report all known structure files.
     * @return a collection of filenames
     */
    Set<String> reportKnownFiles();
}
