package org.rcsb.strucmotif.io.read;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.InputStream;
import java.util.Collection;

/**
 * Specifies how structure data is read.
 */
public interface StructureReader {
    /**
     * Read a selected range of {@link Residue} instances.
     * @param inputStream the source of data, assumed to be following the mmCIF schema
     * @param selection a collection of {@link ResidueSelection} instances - null will return whole structure
     * @return the corresponding {@link Structure}
     */
    Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection);

    /**
     * Read everything from an input stream.
     * @param inputStream the source of data, assumed to be following the mmCIF schema
     * @return the corresponding {@link Structure}
     */
    default Structure readFromInputStream(InputStream inputStream) {
        return readFromInputStream(inputStream, null);
    }
}
