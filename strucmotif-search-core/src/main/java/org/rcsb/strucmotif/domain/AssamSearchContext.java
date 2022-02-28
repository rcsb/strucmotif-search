package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.domain.query.AssamParameters;
import org.rcsb.strucmotif.domain.query.AssamQueryStructure;
import org.rcsb.strucmotif.domain.query.AssamSearchQuery;
import org.rcsb.strucmotif.domain.result.AssamHit;
import org.rcsb.strucmotif.domain.result.AssamMotifSearchResult;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AssamSearchContext implements SearchContext<AssamSearchQuery, AssamParameters, AssamQueryStructure, AssamMotifSearchResult, AssamHit> {
    private static final Logger logger = LoggerFactory.getLogger(AssamSearchContext.class);
    private final MotifSearchRuntime runtime;
    private final MotifSearchConfig config;
    private final AssamSearchQuery query;
    private final AssamMotifSearchResult result;

    public AssamSearchContext(MotifSearchRuntime motifSearchRuntime, MotifSearchConfig motifSearchConfig, AssamSearchQuery query) {
        this.runtime = motifSearchRuntime;
        this.config = motifSearchConfig;
        this.query = query;
        this.result = initializeResultContainer();
    }

    private AssamMotifSearchResult initializeResultContainer() {
        AssamQueryStructure queryStructure = query.getQueryStructure();
        AssamParameters parameters = query.getParameters();
        logger.info("[{}] Query: {} with {}",
                query.hashCode(),
                queryStructure.getStructureIdentifier(),
                queryStructure.getIndexSelections());
        logger.info("[{}] Exchanges: {}, Tolerances: [{}, {}, {}], Atom Pairing Scheme: {}, RMSD Cutoff: {}, Limit: {}",
                query.hashCode(),
                query.getExchanges(),
                parameters.getBackboneDistanceTolerance(),
                parameters.getSideChainDistanceTolerance(),
                parameters.getAngleTolerance(),
                parameters.getAtomPairingScheme(),
                parameters.getRmsdCutoff(),
                parameters.getLimit());

        return new AssamMotifSearchResult();
    }

    @Override
    public MotifSearchRuntime getRuntime() {
        return runtime;
    }

    @Override
    public MotifSearchConfig getConfig() {
        return config;
    }

    @Override
    public AssamSearchQuery getQuery() {
        return query;
    }

    /**
     * Dispatch this query and perform the corresponding search.
     * @return the result container
     */
    public AssamMotifSearchResult run() {
        return runtime.performSearch(this);
    }

    /**
     * Dispatch this query and consume each accepted hit on-the-fly. This doesn't keep hits in memory.
     * @param hitConsumer terminal operation to perform on accepted hits
     */
    public void runAndConsume(Consumer<AssamHit> hitConsumer) {
        getRuntime().performSearch(this, hitConsumer);
    }

    /**
     * Dispatch this query and write each accepted hit to a file. Overwrites existing files, doesn't keep hits in memory.
     * @param path destination
     */
    public void runAndWriteToPath(Path path) {
        try {
            Files.writeString(path, composeHeader(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        runAndConsume(hit -> {
            String output = composeOutput(hit, config.getDecimalPlacesScore(), config.getDecimalPlacesMatrix());
            try {
                Files.writeString(path, output, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static final String COLUMN_DELIMITER = ",";
    private static final String VALUE_DELIMITER = ";";
    private String composeHeader() {
        return "pdb_id" + COLUMN_DELIMITER +
                "assembly_id" + COLUMN_DELIMITER +
                "rmsd_score" + COLUMN_DELIMITER +
                "residue_ids" + COLUMN_DELIMITER +
                "residue_types" + COLUMN_DELIMITER +
                "transformation" + System.lineSeparator();
    }

    private String composeOutput(AssamHit hit, int scorePlaces, int matrixPlaces) {
        float[] original = hit.getTransformation().getFlattenedTransformation();
        List<Float> matrix = new ArrayList<>();
        for (float v : original) {
            matrix.add(truncate(v, matrixPlaces));
        }

        return hit.getStructureIdentifier() + COLUMN_DELIMITER +
                hit.getAssemblyIdentifier() + COLUMN_DELIMITER +
                truncate(hit.getRootMeanSquareDeviation(), scorePlaces) + COLUMN_DELIMITER +
                toString(hit.getLabelSelections()) + COLUMN_DELIMITER +
                toString(hit.getResidueTypes()) + COLUMN_DELIMITER +
                toString(matrix) + System.lineSeparator();
    }

    private String toString(Collection<?> collection) {
        return collection.stream().map(Object::toString).collect(Collectors.joining(VALUE_DELIMITER, "[", "]"));
    }

    /**
     * Truncate floats to any number of decimal places.
     * @param value the raw value
     * @return a float with the specified number of decimal places
     */
    private float truncate(float value, int decimalPlaces) {
        return BigDecimal.valueOf(value)
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .floatValue();
    }

    @Override
    public AssamMotifSearchResult getResult() {
        return result;
    }
}
