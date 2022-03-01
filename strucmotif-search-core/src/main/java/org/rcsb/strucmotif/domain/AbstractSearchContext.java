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

public abstract class AbstractSearchContext<Q extends SearchQuery<P, S>, P extends Parameters, S extends QueryStructure, R extends SearchResult<H>, H extends Hit> implements SearchContext<Q, P, S, R, H> {
    protected static final String COLUMN_DELIMITER = ",";
    protected static final String VALUE_DELIMITER = ";";

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

    protected abstract String composeHeader();

    protected abstract String composeOutput(H hit);

    protected String toString(Collection<?> collection) {
        return collection.stream().map(Object::toString).collect(Collectors.joining(VALUE_DELIMITER, "[", "]"));
    }

    /**
     * Truncate floats to any number of decimal places.
     * @param value the raw value
     * @return a float with the specified number of decimal places
     */
    protected float truncate(float value, int decimalPlaces) {
        return BigDecimal.valueOf(value)
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .floatValue();
    }
}
