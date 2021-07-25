package org.rcsb.strucmotif.align;

import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;

import java.util.List;
import java.util.Map;

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
    AlignmentResult align(List<Map<LabelAtomId, float[]>> reference, List<Map<LabelAtomId, float[]>> candidate, AtomPairingScheme atomPairingScheme);
}
