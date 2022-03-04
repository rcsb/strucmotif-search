package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Parameters of a 'detect-motifs' run.
 */
public class MotifParameters implements Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final float rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final MotifPruner motifPruner;

    /**
     * Construct motif parameters.
     * @param backboneDistanceTolerance backbone tolerance
     * @param sideChainDistanceTolerance side-chain tolerance
     * @param angleTolerance angle tolerance
     * @param rmsdCutoff maximum RMSD
     * @param atomPairingScheme how to pair atoms
     * @param motifPruner how to prune motifs
     */
    public MotifParameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, float rmsdCutoff, AtomPairingScheme atomPairingScheme, MotifPruner motifPruner) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.motifPruner = motifPruner;
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
}
