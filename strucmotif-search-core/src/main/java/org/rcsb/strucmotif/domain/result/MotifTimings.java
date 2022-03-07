package org.rcsb.strucmotif.domain.result;

/**
 * Timings in the context of motif queries.
 */
public class MotifTimings implements Timings {
    private final Timer query;

    /**
     * Constructs a new timer instance. Immediately starts the query timer.
     */
    public MotifTimings() {
        this.query = new Timer();
        queryStart();
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
