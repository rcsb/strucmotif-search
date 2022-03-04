package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.query.SearchQuery;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SearchResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Shared properties of a {@link SearchContext}.
 * @param <Q> the query type
 * @param <P> the parameter type
 * @param <S> the structure definition type
 * @param <R> the result type
 * @param <H> the result hit type
 */
public abstract class AbstractSearchContext<Q extends SearchQuery<P, S>, P extends Parameters, S extends QueryStructure, R extends SearchResult<H>, H extends Hit> implements SearchContext<Q, P, S, R, H> {
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

    /**
     * Create a context, will assign a reasonable identifier.
     */
    protected AbstractSearchContext() {
        this.id = String.valueOf(hashCode());
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
