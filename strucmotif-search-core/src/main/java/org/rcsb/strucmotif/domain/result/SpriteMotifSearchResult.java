package org.rcsb.strucmotif.domain.result;

import java.util.ArrayList;
import java.util.List;

public class SpriteMotifSearchResult implements SearchResult<SpriteHit> {
    private final Timings timings;
    private final List<SpriteHit> hits;

    /**
     * Construct a result container.
     */
    public SpriteMotifSearchResult() {
        this.timings = new Timings();
        this.hits = new ArrayList<>();
    }

    /**
     * Associated timings.
     * @return timings object
     */
    @Override
    public Timings getTimings() {
        return timings;
    }

    @Override
    public List<SpriteHit> getHits() {
        return hits;
    }
}
