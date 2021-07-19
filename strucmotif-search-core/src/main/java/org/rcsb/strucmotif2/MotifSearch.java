package org.rcsb.strucmotif2;

import org.rcsb.strucmotif2.domain.query.QueryBuilder;

/**
 * The entry point to perform motif searches.
 */
public class MotifSearch {
    private final QueryBuilder queryBuilder;
    private static final MotifSearch INSTANCE = new MotifSearch();

    private MotifSearch() {
        MotifSearchApplication.main(new String[0]);
        this.queryBuilder = MotifSearchApplication.queryBuilder;
    }

    /**
     * Start a new query using the {@link QueryBuilder}.
     * @return a fresh builder instance
     */
    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }
}
