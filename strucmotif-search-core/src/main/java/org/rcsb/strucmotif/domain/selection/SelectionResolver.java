package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

/**
 * A selection resolver is a bidirectional map that provides the mapping between a selector (index or label) and
 * a residue. Effectively, this can be used to select residue from structures. It is especially efficient for
 * 'small' structures such as motif definitions or target structures. The concept is to provide the associated
 * {@link Structure} during construction.
 * @param <S> the compatible selector for this resolver
 */
public interface SelectionResolver<S extends Selection> {
    /**
     * Select a component from a structure.
     * @param selector the criteria to match
     * @return the residue if present, null otherwise
     */
    Residue resolve(S selector);

    /**
     * Retrieve the selector for a residue.
     * @param residue the residue to map
     * @return the corresponding selector
     */
    S resolve(Residue residue);
}
