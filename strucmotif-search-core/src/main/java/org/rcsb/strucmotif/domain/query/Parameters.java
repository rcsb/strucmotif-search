package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Immutable instance capturing all parameters of a given search job.
 */
public class Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final float rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;
    private final int limit;
    static final int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_ANGLE_TOLERANCE = 1;

    Parameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, int resultLimit) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
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
     * At which RMSD are hits filtered?
     * @return the rmsd cutoff value
     */
    public float getRmsdCutoff() {
        return rmsdCutoff;
    }

    /**
     * How are atoms paired?
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
