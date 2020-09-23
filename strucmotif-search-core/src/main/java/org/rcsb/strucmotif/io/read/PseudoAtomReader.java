package org.rcsb.strucmotif.io.read;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.nio.file.Path;
import java.util.Collection;

public interface PseudoAtomReader {
    /**
     * Read sparse coordinates for a small number of {@link IndexSelection} instances.
     * @param structureIdentifier the structure to read
     * @param source the structure directory to read
     * @param selection which indices to retrieve
     * @return a structure containing all selected residues
     */
    Structure read(StructureIdentifier structureIdentifier, Path source, Collection<IndexSelection> selection);
}
