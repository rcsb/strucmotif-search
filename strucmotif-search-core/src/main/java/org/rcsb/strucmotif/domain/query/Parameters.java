package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

public interface Parameters {
    int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    int DEFAULT_ANGLE_TOLERANCE = 1;

    int getBackboneDistanceTolerance();

    int getSideChainDistanceTolerance();

    int getAngleTolerance();

    float getRmsdCutoff();

    boolean hasRmsdCutoff();

    AtomPairingScheme getAtomPairingScheme();

    MotifPruner getMotifPruner();
}
