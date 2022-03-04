package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.Structure;

/**
 * References the structure that is the basis of a query.
 */
public interface QueryStructure {
    /**
     * The identifier of this structure.
     * @return a String
     */
    String getStructureIdentifier();

    /**
     * The structure data.
     * @return {@link Structure} instance
     */
    Structure getStructure();
}
