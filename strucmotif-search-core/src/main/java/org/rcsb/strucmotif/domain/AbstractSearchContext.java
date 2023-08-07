package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.core.QueryExecutionException;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Shared properties of a {@link SearchContext}.
 * @param <P> the parameter type
 * @param <S> the structure definition type
 * @param <H> the result hit type
 */
public abstract class AbstractSearchContext<P extends Parameters, S extends QueryStructure, H extends Hit> implements SearchContext<P, S, H> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractSearchContext.class);
    /**
     * The delimiter between columns.
     */
    protected static final String COLUMN_DELIMITER = ",";
    /**
     * The delimiter between values within a cell (think arrays/lists).
     */
    protected static final String VALUE_DELIMITER = ";";
    /**
     * The identifier of this context.
     */
    protected final String id;
    private boolean done;
    private final ExecutorService executorService;

    /**
     * Create a context, will assign a reasonable identifier.
     * @param executorService will run associated tasks
     */
    protected AbstractSearchContext(ExecutorService executorService) {
        this.id = String.valueOf(hashCode());
        this.done = false;
        this.executorService = executorService;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void runAndWriteToPath(Path path) {
        try {
            Files.writeString(path, composeHeader(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        runAndConsume(hit -> {
            String output = composeOutput(hit);
            try {
                Files.writeString(path, output, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void markAsDone() {
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public <R> R tryExecute(Callable<R> task) {
        try {
            return executorService.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Async task was interrupted", e);
            throw new QueryExecutionException("Async task was interrupted");
        } catch (ExecutionException e) {
            logger.warn("Async task failed to execute", e);
            throw new QueryExecutionException("Async task failed to execute");
        }
    }

    /**
     * When writing result CSVs: How does the header look like?
     * @return the header line
     */
    protected abstract String composeHeader();

    /**
     * When writing result CSVs: How does an individual line look like?
     * @param hit write results for this
     * @return a result line
     */
    protected abstract String composeOutput(H hit);

    /**
     * Convert a collection to a string.
     * @param collection input
     * @return a String representing each element
     */
    protected String toString(Collection<?> collection) {
        return collection.stream().map(Object::toString).collect(Collectors.joining(VALUE_DELIMITER, "[", "]"));
    }

    /**
     * Truncate floats to any number of decimal places.
     * @param value the raw value
     * @param decimalPlaces how many places to show
     * @return a float with the specified number of decimal places
     */
    protected float truncate(float value, int decimalPlaces) {
        return BigDecimal.valueOf(value)
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .floatValue();
    }
}
