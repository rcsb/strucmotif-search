package org.rcsb.strucmotif.domain.result;

import java.util.List;

/**
 * The results of a search run.
 */
public interface SearchResult<H extends Hit> {
    /**
     * Associated timings.
     * @return captured timings
     */
    Timings getTimings();

    /**
     * All accepted hits.
     * @return a collection of hits
     */
    List<H> getHits();
}
