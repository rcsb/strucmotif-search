package org.rcsb.strucmotif2.core;

import org.rcsb.strucmotif2.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif2.domain.result.Hit;
import org.rcsb.strucmotif2.domain.result.MotifSearchResult;

import java.util.function.Consumer;

/**
 * Performs motif search queries and returns the corresponding result object.
 */
public interface MotifSearchRuntime {
    /**
     * Performs a structural motif search run for a given query.
     * @param query the query, specifying motif and all parameters
     * @return the result container
     */
    MotifSearchResult performSearch(MotifSearchQuery query);

    /**
     * Performs a structural motif search for a given query.
     * @param query the query, specifying motif and all parameters
     * @param consumer some terminal operation that will be applied to each accepted hit
     */
    void performSearch(MotifSearchQuery query, Consumer<Hit> consumer);
}
