package org.rcsb.strucmotif2.io.write;

import org.rcsb.cif.schema.mm.MmCifFile;

import java.nio.file.Path;

/**
 * The contract to write renumbered structure data. Renumbering refers to enforcing a 'consistent' numbering of residues
 * that will allows to access each {@link org.rcsb.strucmotif2.domain.structure.Residue} by a
 * {@link org.rcsb.strucmotif2.domain.selection.LabelSelection}.
 */
public interface RenumberedStructureWriter {
    /**
     * Write structure data to the file system.
     * @param source a MmCifFile to process
     * @param destination where to write data
     */
    void write(MmCifFile source, Path destination);
}
