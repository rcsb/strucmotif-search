package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.AtomPairingScheme;

/**
 * Immutable instance capturing all parameters of a given search job.
 */
public class Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final double scoreCutoff;
    private final MotifPruner motifPruner;
    private final AtomPairingScheme atomPairingScheme;
    private final int limit;
    static final int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_ANGLE_TOLERANCE = 1;
    static final double DEFAULT_SCORE_CUTOFF = 3.0;

    Parameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, double scoreCutoff, MotifPruner motifPruner, AtomPairingScheme atomPairingScheme, int resultLimit) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.scoreCutoff = scoreCutoff == 0 ? Double.MAX_VALUE : scoreCutoff;
        this.motifPruner = motifPruner;
        this.atomPairingScheme = atomPairingScheme;
        this.limit = resultLimit == 0 ? Integer.MAX_VALUE : resultLimit;
    }

    /**
     * The specified backbone distance tolerance value.
     * @return the tolerance value
     */
    public int getBackboneDistanceTolerance() {
        return backboneDistanceTolerance;
    }

    /**
     * The specified side-chain distance tolerance value.
     * @return the tolerance value
     */
    public int getSideChainDistanceTolerance() {
        return sideChainDistanceTolerance;
    }

    /**
     * The specified angle tolerance value.
     * @return the tolerance value
     */
    public int getAngleTolerance() {
        return angleTolerance;
    }

    /**
     * At which score are hits filtered?
     * @return the cutoff value
     */
    public double getScoreCutoff() {
        return scoreCutoff;
    }

    /**
     * The motif pruning strategy applied.
     * @return motif pruning impl
     */
    public MotifPruner getMotifPruner() {
        return motifPruner;
    }

    /**
     * The atom pairing strategy applied.
     * @return atom pairing scheme
     */
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    public int getLimit() {
        return limit;
    }

    public boolean hasLimit() {
        return limit != Integer.MAX_VALUE;
    }
}
