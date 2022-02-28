package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

public class SpriteParameters implements Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final float rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;
    private final boolean undefinedAssemblies;
    static final int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_ANGLE_TOLERANCE = 1;

    SpriteParameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner, boolean undefinedAssemblies) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
        this.undefinedAssemblies = undefinedAssemblies;
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
     * Report if results will be filtered by RMSD.
     * @return true if search will omit hits above a certain threshold.
     */
    public boolean hasRmsdCutoff() {
        return rmsdCutoff != Float.MAX_VALUE;
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
     * Report if undefined assemblies (if they were indexed) can be returned.
     * @return true if this search will allow undefined assemblies
     */
    public boolean isUndefinedAssemblies() {
        return undefinedAssemblies;
    }
}
