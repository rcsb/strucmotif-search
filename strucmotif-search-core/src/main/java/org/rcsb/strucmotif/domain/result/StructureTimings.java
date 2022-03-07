package org.rcsb.strucmotif.domain.result;

/**
 * Timings in the context of structure queries.
 */
public class StructureTimings implements Timings {
    private final Timer query;
    private final Timer paths;
    private final Timer scoreHits;

    /**
     * Constructs a new timer instance. Immediately starts the query timer.
     */
    public StructureTimings() {
        this.query = new Timer();
        this.paths = new Timer();
        this.scoreHits = new Timer();
        queryStart();
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
     * Start the hit scoring timer.
     */
    public void scoreHitsStart() {
        scoreHits.start();
    }

    /**
     * Stop the hit scoring timer.
     */
    public void scoreHitsStop() {
        this.scoreHits.stop();
    }

    /**
     * Get the hit scoring time in ms.
     * @return a long
     */
    public long getScoreHitsTime() {
        return scoreHits.getMillisecondTime();
    }

    @Override
    public void queryStart() {
        query.start();
    }

    @Override
    public void queryStop() {
        query.stop();
    }

    @Override
    public long getQueryTime() {
        return query.getMillisecondTime();
    }
}
