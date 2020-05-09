package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;

/**
 * The difference between identifiers like {@link ResidueIdentifier} and
 * selectors like {@link IndexSelection} is that selections need to be aware of more information (namely, they need to
 * reference the correct chain/assembly operation).
 */
public interface Selection {
    int getAssemblyId();
}
