package org.rcsb.strucmotif.domain.query;

public interface SearchQuery<P extends Parameters, S extends QueryStructure> {
    P getParameters();

    S getQueryStructure();
}
