package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.structure.Residue;

/**
 * Identifies a {@link Residue} uniquely in a complete structure.
 */
public interface ResidueSelection {
    int getAssemblyId();
}
