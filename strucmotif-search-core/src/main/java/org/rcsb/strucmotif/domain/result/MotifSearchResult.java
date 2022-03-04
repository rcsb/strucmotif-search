package org.rcsb.strucmotif.domain.result;

import java.util.ArrayList;
import java.util.List;

/**
 * The results of a motif search run.
 */
public class MotifSearchResult implements SearchResult<MotifHit> {
    private final Timings timings;
    private final List<MotifHit> hits;

    /**
     * Construct a result container.
     */
    public MotifSearchResult() {
        this.timings = new Timings();
        this.hits = new ArrayList<>();
    }

    @Override
    public Timings getTimings() {
        return timings;
    }

    @Override
    public List<MotifHit> getHits() {
        return hits;
    }
}
