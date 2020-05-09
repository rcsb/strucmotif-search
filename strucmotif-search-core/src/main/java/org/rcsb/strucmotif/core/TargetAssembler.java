package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.result.MotifSearchResult;

import java.util.concurrent.ExecutionException;

/**
 * Where the magic happens. Lookup all word occurrences which need to be fulfilled and combine them in the most
 * efficient way implementable.
 */
public interface TargetAssembler {
    /**
     * Search: i.e. find all paths through all target structures which reasonably resemble the structure of the query
     * motif.
     * @param response the container to work on
     */
    void assemble(MotifSearchResult response) throws ExecutionException, InterruptedException;
}
