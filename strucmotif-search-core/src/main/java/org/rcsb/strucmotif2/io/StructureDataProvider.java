package org.rcsb.strucmotif2.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.selection.ResidueSelection;
import org.rcsb.strucmotif2.domain.structure.Residue;
import org.rcsb.strucmotif2.domain.structure.Structure;

import java.io.InputStream;
import java.util.Collection;

/**
 * Access structure data.
 */
public interface StructureDataProvider {
    /**
     * Read a selected range of {@link Residue} instances.
     * @param inputStream the source of data, assumed to be following the mmCIF schema
     * @param selection a collection of {@link ResidueSelection} instances - null will return whole structure
     * @return the corresponding {@link Structure}
     */
    Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection);

    /**
     * Read everything from an input stream.
     * @param inputStream the source of data, assumed to be following the mmCIF schema
     * @return the corresponding {@link Structure}
     */
    default Structure readFromInputStream(InputStream inputStream) {
        return readFromInputStream(inputStream, null);
    }

    /**
     * Read a selected range from a renumbered structure.
     * @param structureIdentifier the structure to read
     * @param selection a collection of {@link ResidueSelection} instances - null will return whole structure
     * @return the corresponding {@link Structure}
     */
    Structure readRenumbered(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection);

    /**
     * Read a renumbered structure entirely.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    default Structure readRenumbered(StructureIdentifier structureIdentifier) {
        return readRenumbered(structureIdentifier, null);
    }

    /**
     * Read a selected range from an original structure.
     * @param structureIdentifier the structure to read
     * @param selection a collection of {@link ResidueSelection} instances - null will return whole structure
     * @return the corresponding {@link Structure}
     */
    Structure readOriginal(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection);

    /**
     * Read an original structure entirely.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    default Structure readOriginal(StructureIdentifier structureIdentifier) {
        return readOriginal(structureIdentifier, null);
    }

    /**
     * Try to read 'any' structure data. Priorities are 'data-source', 'root-path', and 'bcif-fetch-url'.
     * @param structureIdentifier the structure to read
     * @param selection a collection of {@link ResidueSelection} instances - null will return whole structure
     * @return the corresponding {@link Structure}
     */
    Structure readSome(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection);

    /**
     * Read some structure entirely.
     * @param structureIdentifier the structure to read
     * @return the corresponding {@link Structure}
     */
    default Structure readSome(StructureIdentifier structureIdentifier) {
        return readSome(structureIdentifier, null);
    }

    /**
     * Write a renumbered structure.
     * @param structureIdentifier the structure identifier to write
     * @param mmCifFile the data source
     */
    void writeRenumbered(StructureIdentifier structureIdentifier, MmCifFile mmCifFile);

    /**
     * Drop information on a renumbered structure.
     * @param structureIdentifier the structure identifier to remove
     */
    void deleteRenumbered(StructureIdentifier structureIdentifier);

    /**
     * Acquire the input stream of an original structure.
     * @param structureIdentifier the structure identifier to read
     * @return the corresponding input stream
     */
    InputStream getOriginalInputStream(StructureIdentifier structureIdentifier);
}
