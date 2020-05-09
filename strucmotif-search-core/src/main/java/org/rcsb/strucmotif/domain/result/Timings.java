package org.rcsb.strucmotif.domain.result;

/**
 * A helper class keeping track of internal timings. Useful for development and diagnosis. All but the query timer need
 * to be started at an appropriate time during the query. 'Overall' query timer is started upon creation of this class.
 * All timers need to be explicitly stopped.
 */
public class Timings {
    private final Timer query;
    private final Timer paths;
    private final Timer structures;

    /**
     * Constructs a new timer instance. Immediately starts the query timer.
     */
    public Timings() {
        this.query = new Timer();
        this.paths = new Timer();
        this.structures = new Timer();
        this.query.start();
    }

    /**
     * Start the path assembly timer.
     */
    public void pathsStart() {
        paths.start();
    }

    /**
     * Stop the path assembly timer.
     */
    public void pathsStop() {
        paths.stop();
    }

    /**
     * Get the path assembly time in ms.
     * @return a long
     */
    public long getPathsTime() {
        return paths.getMillisecondTime();
    }

    /**
     * Start the structure parsing timer.
     */
    public void structuresStart() {
        structures.start();
    }

    /**
     * Stop the structure parsing timer.
     */
    public void structuresStop() {
        this.structures.stop();
    }

    /**
     * Get the structure parsing time in ms.
     * @return a long
     */
    public long getStructuresTime() {
        return structures.getMillisecondTime();
    }

    /**
     * Stop the overall query timer.
     */
    public void queryStop() {
        query.stop();
    }

    /**
     * Get the overall query timer in ms.
     * @return a long
     */
    public long getQueryTime() {
        return query.getMillisecondTime();
    }
}
