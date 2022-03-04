package org.rcsb.strucmotif.domain.query;

/**
 * Summarizes a query.
 * @param <P> parameter type
 * @param <S> query structure type
 */
public interface SearchQuery<P extends Parameters, S extends QueryStructure> {
    /**
     * Access to the parameters used.
     * @return a {@link Parameters} instance
     */
    P getParameters();

    /**
     * Access to the underlying query structure.
     * @return a {@link QueryStructure} instance
     */
    S getQueryStructure();
}
