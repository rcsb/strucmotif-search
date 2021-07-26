package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.InputStream;

/**
 * Read structures from CIF files.
 */
public interface StructureReader {
    /**
     * Read from a InputStream and parse the corresponding Structure.
     * @param inputStream the source
     * @return the parsed Structure instance
     */
    Structure readFromInputStream(InputStream inputStream);
}
