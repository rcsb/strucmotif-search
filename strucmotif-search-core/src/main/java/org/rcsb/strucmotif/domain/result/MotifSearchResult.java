package org.rcsb.strucmotif.domain.result;

import java.util.ArrayList;
import java.util.List;

/**
 * The results of a motif search run.
 */
public class MotifSearchResult implements SearchResult<MotifHit> {
    private final MotifTimings timings;
    private List<MotifHit> hits;

    /**
     * Construct a result container.
     */
    public MotifSearchResult() {
        this.timings = new MotifTimings();
        this.hits = new ArrayList<>();
    }

    @Override
    public MotifTimings getTimings() {
        return timings;
    }

    /**
     * Update the collection of accepted hits.
     * @param hits a collection of hits
     */
    public void setHits(List<MotifHit> hits) {
        this.hits = hits;
    }

    @Override
    public List<MotifHit> getHits() {
        return hits;
    }
}
