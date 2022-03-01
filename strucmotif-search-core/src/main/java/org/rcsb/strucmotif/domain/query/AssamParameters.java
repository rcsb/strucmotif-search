package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Immutable instance capturing all parameters of a given search job.
 */
public class AssamParameters implements Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final float rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;
    private final int limit;
    private final boolean undefinedAssemblies;

    AssamParameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, int resultLimit, boolean undefinedAssemblies) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
        this.limit = resultLimit;
        this.undefinedAssemblies = undefinedAssemblies;
    }

    /**
     * The specified backbone distance tolerance value.
     * @return the tolerance value
     */
    @Override
    public int getBackboneDistanceTolerance() {
        return backboneDistanceTolerance;
    }

    /**
     * The specified side-chain distance tolerance value.
     * @return the tolerance value
     */
    @Override
    public int getSideChainDistanceTolerance() {
        return sideChainDistanceTolerance;
    }

    /**
     * The specified angle tolerance value.
     * @return the tolerance value
     */
    @Override
    public int getAngleTolerance() {
        return angleTolerance;
    }

    /**
     * At which RMSD are hits filtered?
     * @return the rmsd cutoff value
     */
    @Override
    public float getRmsdCutoff() {
        return rmsdCutoff;
    }

    /**
     * Report if results will be filtered by RMSD.
     * @return true if search will omit hits above a certain threshold.
     */
    @Override
    public boolean hasRmsdCutoff() {
        return rmsdCutoff != Float.MAX_VALUE;
    }

    /**
     * How are atoms paired?
     * @return the atom pairing scheme
     */
    @Override
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    /**
     * The motif pruning strategy applied.
     * @return motif pruning impl
     */
    @Override
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

    /**
     * Report if undefined assemblies (if they were indexed) can be returned.
     * @return true if this search will allow undefined assemblies
     */
    public boolean isUndefinedAssemblies() {
        return undefinedAssemblies;
    }
}
