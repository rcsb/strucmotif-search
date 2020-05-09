package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;

public interface InternalMotifSearch {
    /**
     * Performs a structural motif search run for a given query.
     * @param query the query, specifying motif and all parameters
     * @return the result container
     */
    MotifSearchResult performSearch(MotifSearchQuery query);
}
