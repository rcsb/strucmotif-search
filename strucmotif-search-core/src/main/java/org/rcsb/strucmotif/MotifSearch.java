package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.QueryBuilder;

/**
 * The entry point to perform motif searches. Handles configuration, sets up dependency injection and exposes
 * configuration.
 */
public class MotifSearch {
    private final QueryBuilder queryBuilder;
    private static final MotifSearch INSTANCE = new MotifSearch();

    private MotifSearch() {
        // TODO resolve and manage bindings
        this.queryBuilder = null;
    }

    /**
     * Start a new query using the {@link QueryBuilder}.
     * @return a fresh builder instance
     */
    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }
}
