package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

/**
 * 'Root' of the data structure of 3D structure data. Hierarchy is {@link Structure} &gt; {@link Chain} &gt;
 * {@link Residue} &gt; {@link Atom}. However, this is not a typical data structure to represent structure data. It is
 * somewhat unsafe to use. Construction may also be not trivial, therefore constructors are package-private and
 * everything is wrapped by {@link StructureFactory}.
 */
public class Structure {
    private final StructureIdentifier structureIdentifier;
    private final List<Chain> chains;

    /**
     * Construct a new structure.
     * @param structureIdentifier the PDB-id
     * @param chains all chains present
     */
    Structure(StructureIdentifier structureIdentifier, List<Chain> chains) {
        this.structureIdentifier = structureIdentifier;
        this.chains = chains;
    }

    /**
     * Reports the PDB-id of this structure.
     * @return the structure identifier
     */
    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Reports all chains.
     * @return a list of chains
     */
    public List<Chain> getChains() {
        return chains;
    }

    @Override
    public String toString() {
        return structureIdentifier + " chains: " + chains;
    }
}
