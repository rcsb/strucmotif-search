package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Immutable instance capturing all parameters of a given search job.
 */
public class StructureParameters implements Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final float rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;
    private final int limit;
    private final int timeout;

    /**
     * Construct structure parameters.
     * @param backboneDistanceTolerance backbone tolerance
     * @param sideChainDistanceTolerance side-chain tolerance
     * @param angleTolerance angle tolerance
     * @param rmsdCutoff maximum RMSD
     * @param atomPairingScheme how to pair atoms
     * @param motifPruner how to prune motifs
     * @param resultLimit stop after this many hits
     * @param timeout timeout in ms
     */
    public StructureParameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, int resultLimit, int timeout) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
        this.limit = resultLimit;
        this.timeout = timeout;
    }

    /**
     * Employed backbone distance tolerance.
     * @return an int
     */
    public int getBackboneDistanceTolerance() {
        return backboneDistanceTolerance;
    }

    /**
     * Employed side-chain distance tolerance.
     * @return an int
     */
    public int getSideChainDistanceTolerance() {
        return sideChainDistanceTolerance;
    }

    /**
     * Employed angle tolerance.
     * @return an int
     */
    public int getAngleTolerance() {
        return angleTolerance;
    }

    /**
     * Maximum RMSD up to which hits will be accepted, can be 'infinite'.
     * @return a float
     */
    public float getRmsdCutoff() {
        return rmsdCutoff;
    }

    /**
     * If filtering by RMSD will happen.
     * @return true if there's a cutoff
     */
    public boolean hasRmsdCutoff() {
        return rmsdCutoff != Float.MAX_VALUE;
    }

    /**
     * Which atoms will get aligned.
     * @return an {@link AtomPairingScheme}
     */
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    /**
     * The associated motif pruning strategy.
     * @return a {@link MotifPruner}
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

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public boolean hasTimeout() {
        return timeout != Integer.MAX_VALUE;
    }
}
