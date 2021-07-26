package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;

import java.util.List;
import java.util.Map;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class HitScorer {
    private final List<Map<LabelAtomId, float[]>> queryResidues;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignmentService;

    /**
     * Construct a query-specific hit scorer.
     * @param queryResidues the reference structure
     * @param atomPairingScheme how to pair atoms?
     * @param alignmentService alignment service
     */
    public HitScorer(List<Map<LabelAtomId, float[]>> queryResidues, AtomPairingScheme atomPairingScheme, AlignmentService alignmentService) {
        this.queryResidues = queryResidues;
        this.atomPairingScheme = atomPairingScheme;
        this.alignmentService = alignmentService;
    }

    /**
     * Returns the used atom pairing scheme (e.g., all-atoms, side-chain, ...)
     * @return an {@link AtomPairingScheme} value
     */
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    /**
     * Align a set of residues to the reference.
     * @param targetResidues collection of residues
     * @return an {@link AlignmentResult}
     */
    public AlignmentResult alignToReference(List<Map<LabelAtomId, float[]>> targetResidues) {
        return alignmentService.align(queryResidues, targetResidues, atomPairingScheme);
    }
}
