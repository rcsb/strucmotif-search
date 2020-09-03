package org.rcsb.strucmotif.io.write;

import org.rcsb.cif.schema.mm.MmCifFile;

/**
 * The contract to write renumberd structure data. For simplicity and consistency, raw binary files are processed
 * (rather than {@link org.rcsb.strucmotif.domain.structure.Structure}).
 */
public interface RenumberedWriter {
    /**
     * Write structure data to the file system.
     * @param container the container to process
     */
    void write(MmCifFile container);
}
