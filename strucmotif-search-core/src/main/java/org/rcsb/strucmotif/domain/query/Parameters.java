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
    private final double rmsdCutoff;
    private final ScoringStrategy scoringStrategy;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;
    private final int limit;
    static final int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_ANGLE_TOLERANCE = 1;

    Parameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, double scoreCutoff, double rmsdCutoff, ScoringStrategy scoringStrategy, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, int resultLimit) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.scoreCutoff = scoreCutoff;
        this.rmsdCutoff = rmsdCutoff;
        this.scoringStrategy = scoringStrategy;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
        this.limit = resultLimit;
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
     * At which RMSD are hits filtered? Only relevant when scored by alignment.
     * @return the rmsd cutoff value
     */
    public double getRmsdCutoff() {
        return rmsdCutoff;
    }

    /**
     * How should hits be scored.
     * @return the scoring strategy
     */
    public ScoringStrategy getScoringStrategy() {
        return scoringStrategy;
    }

    /**
     * How are atoms paired when {@link ScoringStrategy} is alignment-based.
     * @return the atom pairing scheme
     */
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    /**
     * The motif pruning strategy applied.
     * @return motif pruning impl
     */
    public MotifPruner getMotifPruner() {
        return motifPruner;
    }

    /**
     * Terminate the search after this many hits have been accepted.
     * @return an int
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Report if the result count is limited.
     * @return true if search will stop after a certain number of hits has been accepted.
     */
    public boolean hasLimit() {
        return limit != Integer.MAX_VALUE;
    }
}
