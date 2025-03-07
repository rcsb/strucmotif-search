package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;

/**
 * The contract to write optimized structure data that don't contain non-polymers, hydrogen, non-selected models etc.
 */
public interface StructureWriter {
    /**
     * Converts a structure data to its binary representation.
     * @param source a MmCifFile to process
     * @return a byte[] with the file content or null if the structure would be empty
     */
    default byte[] write(MmCifFile source) {
        return write(source, StructureDataProvider.DEFAULT_MODEL_IDENTIFIER);
    }

    /**
     * Converts a structure data to its binary representation.
     * @param source a MmCifFile to process
     * @param modelIdentifier identifier of the model to extract
     * @return a byte[] with the file content or null if the structure would be empty
     */
    byte[] write(MmCifFile source, int modelIdentifier);
}
