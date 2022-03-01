package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.query.SearchQuery;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SearchResult;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface SearchContext<Q extends SearchQuery<P, S>, P extends Parameters, S extends QueryStructure, R extends SearchResult<H>, H extends Hit> {
    MotifSearchRuntime getRuntime();

    MotifSearchConfig getConfig();

    Q getQuery();

    R run();

    void runAndConsume(Consumer<H> hitConsumer);

    /**
     * Dispatch this query and write each accepted hit to a file. Overwrites existing files, doesn't keep hits in memory.
     * @param path destination
     */
    void runAndWriteToPath(Path path);

    R getResult();
}
