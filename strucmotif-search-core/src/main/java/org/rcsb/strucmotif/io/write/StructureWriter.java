package org.rcsb.strucmotif.io.write;

import org.rcsb.cif.schema.mm.MmCifFile;

/**
 * The contract to write structure data. For simplicity and consistency, raw binary files are processed (rather than
 * {@link org.rcsb.strucmotif.domain.structure.Structure}). The implementation will determine where to write the file
 * based on file content.
 */
public interface StructureWriter {
    /**
     * Write structure data to the file system.
     * @param container the container to process
     */
    void write(MmCifFile container);
}
