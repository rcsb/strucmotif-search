package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Parameters of a search run.
 */
public interface Parameters {
    /**
     * Default tolerance value for backbone distances.
     */
    int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    /**
     * Default tolerance value for side-chain distances.
     */
    int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    /**
     * Default tolerance value for angles.
     */
    int DEFAULT_ANGLE_TOLERANCE = 1;

    /**
     * Employed backbone distance tolerance.
     * @return an int
     */
    int getBackboneDistanceTolerance();

    /**
     * Employed side-chain distance tolerance.
     * @return an int
     */
    int getSideChainDistanceTolerance();

    /**
     * Employed angle tolerance.
     * @return an int
     */
    int getAngleTolerance();

    /**
     * Maximum RMSD up to which hits will be accepted, can be 'infinite'.
     * @return a float
     */
    float getRmsdCutoff();

    /**
     * If filtering by RMSD will happen.
     * @return true if there's a cutoff
     */
    boolean hasRmsdCutoff();

    /**
     * Which atoms will get aligned.
     * @return an {@link AtomPairingScheme}
     */
    AtomPairingScheme getAtomPairingScheme();

    /**
     * The associated motif pruning strategy.
     * @return a {@link MotifPruner}
     */
    MotifPruner getMotifPruner();
}
