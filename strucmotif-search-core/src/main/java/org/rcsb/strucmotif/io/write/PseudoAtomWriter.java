package org.rcsb.strucmotif.io.write;

import org.rcsb.strucmotif.domain.structure.Structure;

import java.nio.file.Path;

/**
 * Specifies how structure data of pseudo-atoms is written.
 */
public interface PseudoAtomWriter {
    /**
     * Write an optimized representation of this structure to the file-system at the specified path.
     * @param structure the data to process
     * @param destination the location where it will be written
     */
    void write(Structure structure, Path destination);
}
