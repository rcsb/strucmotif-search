package org.rcsb.strucmotif.domain.result;

import java.util.List;

public interface SearchResult<H extends Hit> {
    Timings getTimings();

    List<H> getHits();
}
