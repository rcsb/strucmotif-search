package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.domain.query.SpriteParameters;
import org.rcsb.strucmotif.domain.query.SpriteQueryStructure;
import org.rcsb.strucmotif.domain.query.SpriteSearchQuery;
import org.rcsb.strucmotif.domain.result.SpriteHit;
import org.rcsb.strucmotif.domain.result.SpriteMotifSearchResult;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
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

public class SpriteSearchContext implements SearchContext<SpriteSearchQuery, SpriteParameters, SpriteQueryStructure, SpriteMotifSearchResult, SpriteHit> {
    private static final Logger logger = LoggerFactory.getLogger(SpriteSearchContext.class);
    private final MotifSearchRuntime runtime;
    private final MotifSearchConfig config;
    private final SpriteSearchQuery query;
    private final SpriteMotifSearchResult result;

    public SpriteSearchContext(MotifSearchRuntime motifSearchRuntime, MotifSearchConfig config, SpriteSearchQuery query) {
        this.runtime = motifSearchRuntime;
        this.config = config;
        this.query = query;
        this.result = initializeResultContainer();
    }

    private SpriteMotifSearchResult initializeResultContainer() {
        SpriteQueryStructure queryStructure = query.getQueryStructure();
        SpriteParameters parameters = query.getParameters();
        logger.info("[{}] Query: {}",
                query.hashCode(),
                queryStructure.getStructureIdentifier());
        logger.info("[{}] Tolerances: [{}, {}, {}], Atom Pairing Scheme: {}, RMSD Cutoff: {}",
                query.hashCode(),
                parameters.getBackboneDistanceTolerance(),
                parameters.getSideChainDistanceTolerance(),
                parameters.getAngleTolerance(),
                parameters.getAtomPairingScheme(),
                parameters.getRmsdCutoff());

        ResidueGraph residueGraph = new ResidueGraph(queryStructure.getStructure(), config, false);

        return new SpriteMotifSearchResult();
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
    public SpriteSearchQuery getQuery() {
        return query;
    }

    /**
     * Dispatch this query and perform the corresponding search.
     * @return the result container
     */
    public SpriteMotifSearchResult run() {
        return runtime.performSearch(this);
    }

    /**
     * Dispatch this query and consume each accepted hit on-the-fly. This doesn't keep hits in memory.
     * @param hitConsumer terminal operation to perform on accepted hits
     */
    public void runAndConsume(Consumer<SpriteHit> hitConsumer) {
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
        return "motif_id" + COLUMN_DELIMITER +
                "rmsd_score" + COLUMN_DELIMITER +
                "residue_ids" + COLUMN_DELIMITER +
                "residue_types" + COLUMN_DELIMITER +
                "transformation" + System.lineSeparator();
    }

    private String composeOutput(SpriteHit hit, int scorePlaces, int matrixPlaces) {
        float[] original = hit.getTransformation().getFlattenedTransformation();
        List<Float> matrix = new ArrayList<>();
        for (float v : original) {
            matrix.add(truncate(v, matrixPlaces));
        }

        return hit.getMotifIdentifier() + COLUMN_DELIMITER +
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
    public SpriteMotifSearchResult getResult() {
        return result;
    }
}
