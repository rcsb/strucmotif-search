package org.rcsb.strucmotif.io.write;

import org.rcsb.cif.schema.mm.MmCifFile;

/**
 * The contract to write renumbered structure data. Renumbering refers to enforcing a 'consistent' numbering of residues
 * that will allows to access each {@link org.rcsb.strucmotif.domain.structure.Residue} by a
 * {@link org.rcsb.strucmotif.domain.selection.IndexSelection}.
 */
public interface RenumberedStructureWriter {
    /**
     * Write structure data to the file system.
     * @param structure the structure to renumber
     */
    void write(MmCifFile mmCifFile);
}
