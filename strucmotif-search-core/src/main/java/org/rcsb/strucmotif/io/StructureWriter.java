package org.rcsb.strucmotif.io;

import org.rcsb.cif.schema.mm.MmCifFile;

import java.nio.file.Path;

/**
 * The contract to write optimized structure data that don't contain non-polymers, hydrogen, model afters #1 etc.
 */
public interface StructureWriter {
    /**
     * Write structure data to the file system.
     * @param source a MmCifFile to process
     * @param destination where to write data
     */
    void write(MmCifFile source, Path destination);
}
