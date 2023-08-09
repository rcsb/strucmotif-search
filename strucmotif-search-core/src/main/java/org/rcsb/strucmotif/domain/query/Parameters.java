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

    /**
     * Timeout after this many milliseconds by throwing a {@link java.util.concurrent.TimeoutException}. No timeout is
     * enforced if set to <code>Integer.MAX_VALUE</code>.
     * @return the timeout specified for this query
     */
    int getTimeout();

    /**
     * Reports whether this query is configured to timeout.
     * @return <code>true</code> if this query will get interrupted after some time
     */
    boolean hasTimeout();
}
