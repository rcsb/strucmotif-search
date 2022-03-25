package org.rcsb.strucmotif.domain.query;

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
}
