package org.rcsb.strucmotif2.domain.selection;

import org.rcsb.strucmotif2.domain.structure.Residue;

/**
 * A selection resolver is a bidirectional map that provides the mapping between a selection ({@link LabelSelection} and
 * a {@link Residue}. Effectively, this can be used to select residues from structures.
 * @param <S> the compatible {@link ResidueSelection} implementation of this resolver
 */
public interface SelectionResolver<S extends ResidueSelection> {
    /**
     * Select a {@link Residue} from a structure.
     * @param selection the criteria to match
     * @return the residue if present, null otherwise
     */
    Residue resolve(S selection);

    /**
     * Retrieve the {@link ResidueSelection} for a residue.
     * @param residue the residue to map
     * @return the corresponding selection
     */
    S resolve(Residue residue);
}
