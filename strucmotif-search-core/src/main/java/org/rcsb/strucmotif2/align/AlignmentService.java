package org.rcsb.strucmotif2.align;

import org.rcsb.strucmotif2.domain.AlignmentResult;
import org.rcsb.strucmotif2.domain.AtomPairingScheme;
import org.rcsb.strucmotif2.domain.structure.Residue;

import java.util.List;

/**
 * Superimpose 2 sets of residues.
 */
public interface AlignmentService {
    /**
     * Aligns 2 sets of residues to one another.
     * @param reference the reference set of residues
     * @param candidate the candidate set of residues to evaluate
     * @param atomPairingScheme the atom names to consider for each residue during alignment
     * @return an Alignment instance which provides the aligned instances, transformation operations and scores
     */
    AlignmentResult align(List<Residue> reference, List<Residue> candidate, AtomPairingScheme atomPairingScheme);
}
