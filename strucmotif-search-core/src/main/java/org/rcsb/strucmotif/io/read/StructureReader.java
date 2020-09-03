package org.rcsb.strucmotif.io.read;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.Selection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.InputStream;
import java.util.Collection;

/**
 * The specification on how to read structure data. All readers handle only binary data. Structures are identified
 * solely by their PDB identifier. Each reader implementation handles resolving the proper paths internally.
 */
public interface StructureReader<S extends Selection> {
    /**
     * Read by id - path is resolved internally.
     * @param structureIdentifier the id to read
     * @return the {@link Structure} read
     */
    default Structure readById(StructureIdentifier structureIdentifier) {
        return readById(structureIdentifier, null);
    }

    /**
     * Read by id with selection.
     * @param structureIdentifier the id to read
     * @param selection the components to parse (ignoring everything else)
     * @return the {@link Structure} read
     */
    Structure readById(StructureIdentifier structureIdentifier, Collection<S> selection);

    Structure readFromInputStream(InputStream inputStream);

    Structure readFromInputStream(InputStream inputStream, Collection<S> selection);
}
