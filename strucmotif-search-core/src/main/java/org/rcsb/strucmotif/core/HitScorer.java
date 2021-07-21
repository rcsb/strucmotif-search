package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

import java.util.List;
import java.util.Map;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class HitScorer {
    private final List<Map<String, float[]>> queryResidues;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignmentService;

    /**
     * Construct a query-specific hit scorer.
     * @param queryResidues the reference structure
     * @param atomPairingScheme how to pair atoms?
     * @param alignmentService alignment service
     */
    public HitScorer(List<Map<String, float[]>> queryResidues, AtomPairingScheme atomPairingScheme, AlignmentService alignmentService) {
        this.queryResidues = queryResidues;
        this.atomPairingScheme = atomPairingScheme;
        this.alignmentService = alignmentService;
    }

    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    public AlignmentResult alignToReference(List<Map<String, float[]>> targetResidues) {
        return alignmentService.align(queryResidues, targetResidues, atomPairingScheme);
    }
}
