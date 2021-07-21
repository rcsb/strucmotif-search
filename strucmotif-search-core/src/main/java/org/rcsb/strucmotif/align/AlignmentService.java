package org.rcsb.strucmotif.align;

import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Superimpose 2 sets of residues.
 */
public interface AlignmentService {
    Set<String> AMBIGUOUS_LABEL_ATOM_IDS = Set.of("NH1", "NH2", "OD1", "OD2", "OE1", "OE2", "CD1", "CD2", "CE1", "CE2", "OG1", "OG2");

    /**
     * Aligns 2 sets of residues to one another.
     * @param reference the reference set of residues
     * @param candidate the candidate set of residues to evaluate
     * @param atomPairingScheme the atom names to consider for each residue during alignment
     * @return an Alignment instance which provides the aligned instances, transformation operations and scores
     */
    AlignmentResult align(List<Map<String, float[]>> reference, List<Map<String, float[]>> candidate, AtomPairingScheme atomPairingScheme);
}
