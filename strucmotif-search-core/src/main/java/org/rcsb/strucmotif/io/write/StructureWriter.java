package org.rcsb.strucmotif.io.write;

/**
 * The contract to write structure data. For simplicity and consistency, raw binary files are processed (rather than
 * {@link org.rcsb.strucmotif.domain.structure.Structure}). The implementation will determine where to write the file
 * based on file content.
 * @param <C> the container which this writer handles
 */
public interface StructureWriter<C> {
    /**
     * Write structure data to the file system.
     * @param container the container to process
     */
    void write(C container);
}
