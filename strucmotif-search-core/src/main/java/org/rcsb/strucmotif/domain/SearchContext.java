package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.StrucmotifRuntime;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.query.SearchQuery;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SearchResult;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * A search context captures all properties and dependencies of a search request.
 * @param <P> the parameter type
 * @param <S> the structure definition type
 * @param <H> the result hit type
 */
public interface SearchContext<P extends Parameters, S extends QueryStructure, H extends Hit> {
    /**
     * An identifier for this context. Useful for logging in multi-threaded environments.
     * @return a String identifier
     */
    String getId();

    /**
     * Access to the global {@link StrucmotifRuntime}.
     * @return the runtime
     */
    StrucmotifRuntime getRuntime();

    /**
     * Access to the global {@link StrucmotifConfig}.
     * @return the config
     */
    StrucmotifConfig getConfig();

    /**
     * Summarizes the query that is defined by this context.
     * @return the query container
     */
    SearchQuery<P, S> getQuery();

    /**
     * Execute this query by dispatching it to the runtime.
     * @return the result container
     */
    SearchResult<H> run();

    /**
     * Execute this query but don't return individual hits, rather consume them immediately. Useful for large result
     * sets that require large amounts of memory otherwise.
     * @param hitConsumer a terminal operation, to be executed on each accepted {@link Hit}
     */
    void runAndConsume(Consumer<H> hitConsumer);

    /**
     * Dispatch this query and write each accepted hit to a file. Overwrites existing files, doesn't keep hits in memory.
     * @param path destination
     */
    void runAndWriteToPath(Path path);

    /**
     * Access to the result container. Will be empty if the search didn't {@link SearchContext#run()} yet.
     * @return the result container
     */
    SearchResult<H> getResult();

    /**
     * Check whether this query has been processed.
     * @return true if results are available
     */
    boolean isDone();

    /**
     * Mark this query has finished (which may be in error state).
     */
    void markAsDone();

    /**
     * Access to the {@link StructureIndexProvider} associated with this context. Will be the global singleton in
     * 'search-for-structures' mode and a single-use, read-only implementation in 'detect-motif' mode.
     * @return a index provider
     */
    StructureIndexProvider getStructureIndexProvider();

    /**
     * Access to the {@link StructureDataProvider} associated with this context. Will be the global singleton in
     * 'search-for-structures' mode and a single-use, read-only implementation in 'detect-motif' mode.
     * @return a data provider
     */
    StructureDataProvider getStructureDataProvider();

    /**
     * Access to the {@link InvertedIndex} associated with this context. Will be the global singleton in
     * 'search-for-structures' mode and a single-use, read-only implementation in 'detect-motif' mode.
     * @return an inverted index
     */
    InvertedIndex getInvertedIndex();

    /**
     * Access to the query-specific executor service that runs this job. It's imperative that this instance used for all
     * parallel operations (i.e., process all parallel streams via this service). Otherwise, the worker threads might
     * not get cancelled after the timeout was reached.
     * @return an {@link ExecutorService}
     */
    ExecutorService getExecutorService();

    /**
     * Execute an async task and hide all the boilerplate handling of interrupts etc.
     * @param task work to do
     * @param <R> anticipated result
     * @return the response
     */
    <R> R tryExecute(Callable<R> task);
}
