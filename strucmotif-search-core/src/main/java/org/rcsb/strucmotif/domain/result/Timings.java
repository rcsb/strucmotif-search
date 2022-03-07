package org.rcsb.strucmotif.domain.result;

/**
 * A helper class keeping track of internal timings. Useful for development and diagnosis. All but the query timer need
 * to be started at an appropriate time during the query. 'Overall' query timer is started upon creation of this class.
 * All timers need to be explicitly stopped. Timers return times in ms.
 */
public interface Timings {
    /**
     * Start the query timer.
     */
    void queryStart();

    /**
     * Stop the query timer.
     */
    void queryStop();

    /**
     * Get the overall query timer in ms.
     * @return a long
     */
    long getQueryTime();
}
