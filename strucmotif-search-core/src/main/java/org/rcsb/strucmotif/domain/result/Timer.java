package org.rcsb.strucmotif.domain.result;

import java.util.concurrent.TimeUnit;

/**
 * Tracks timings.
 */
public class Timer {
    private long start;
    private long time;

    /**
     * Start the time.
     */
    public void start() {
        if (start != 0) {
            throw new IllegalStateException("timer has been started before");
        }
        if (time != 0) {
            throw new IllegalStateException("timer has been stopped before");
        }

        this.start = System.nanoTime();
    }

    /**
     * Stop the time and 'freeze' the measurement for later retrieval/output.
     */
    public void stop() {
        if (start == 0) {
            throw new IllegalStateException("timer was never started");
        }
        if (time != 0) {
            throw new IllegalStateException("timer has been stopped before");
        }

        this.time = System.nanoTime() - start;
    }

    /**
     * Retrieve the measured time in milliseconds.
     * @return a long in ms
     */
    public long getMillisecondTime() {
        return getMeasuredTime(TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieve the measured time.
     * @param timeUnit the {@link TimeUnit} in which to output
     * @return a long of the desired granularity
     */
    public long getMeasuredTime(TimeUnit timeUnit) {
        return timeUnit.convert(time, TimeUnit.NANOSECONDS);
    }
}
