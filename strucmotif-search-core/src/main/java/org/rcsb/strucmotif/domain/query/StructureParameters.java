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
    private final boolean undefinedAssemblies;

    /**
     * Construct structure parameters.
     * @param backboneDistanceTolerance backbone tolerance
     * @param sideChainDistanceTolerance side-chain tolerance
     * @param angleTolerance angle tolerance
     * @param rmsdCutoff maximum RMSD
     * @param atomPairingScheme how to pair atoms
     * @param motifPruner how to prune motifs
     * @param resultLimit stop after this many hits
     * @param undefinedAssemblies allow undefined assemblies in the result set
     */
    public StructureParameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, int resultLimit, boolean undefinedAssemblies) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
        this.limit = resultLimit;
        this.undefinedAssemblies = undefinedAssemblies;
    }

    @Override
    public int getBackboneDistanceTolerance() {
        return backboneDistanceTolerance;
    }

    @Override
    public int getSideChainDistanceTolerance() {
        return sideChainDistanceTolerance;
    }

    @Override
    public int getAngleTolerance() {
        return angleTolerance;
    }

    @Override
    public float getRmsdCutoff() {
        return rmsdCutoff;
    }

    @Override
    public boolean hasRmsdCutoff() {
        return rmsdCutoff != Float.MAX_VALUE;
    }

    @Override
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

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
