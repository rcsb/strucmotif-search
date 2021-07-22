package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

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
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The immutable container for a structural motif query.
 */
public class MotifSearchQuery {
    private final MotifSearchRuntime motifSearchRuntime;
    private final QueryStructure queryStructure;
    private final Parameters parameters;
    private final Map<LabelSelection, Set<ResidueType>> exchanges;
    private final Collection<String> whitelist;
    private final Collection<String> blacklist;
    private final MotifSearchConfig config;

    MotifSearchQuery(MotifSearchRuntime motifSearchRuntime,
                     String structureIdentifier,
                     Structure structure,
                     List<LabelSelection> labelSelections,
                     List<Map<String, float[]>> residues,
                     Parameters parameters,
                     Map<LabelSelection, Set<ResidueType>> exchanges,
                     Collection<String> whitelist,
                     Collection<String> blacklist,
                     MotifSearchConfig config) {
        this.motifSearchRuntime = motifSearchRuntime;
        List<ResiduePairOccurrence> residuePairOccurrences = parameters.getMotifPruner().prune(structure, labelSelections, residues);
        this.queryStructure = new QueryStructure(structureIdentifier, structure, labelSelections, residues, residuePairOccurrences);
        this.parameters = parameters;
        this.exchanges = exchanges;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
        this.config = config;
    }

    /**
     * The structure used to define this query/motif.
     * @return a dedicated implementation wrapping a structure instance
     */
    public QueryStructure getQueryStructure() {
        return queryStructure;
    }

    /**
     * All parameters defined for this search task.
     * @return a parameter instance
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * All specified exchanges.
     * @return a map (key: label selector, value: all allowed component types)
     */
    public Map<LabelSelection, Set<ResidueType>> getExchanges() {
        return exchanges;
    }

    /**
     * Returns the specified search space - no restrictions apply if empty.
     * @return a collection of the search space
     */
    public Collection<String> getWhitelist() {
        return whitelist;
    }

    /**
     * Returns disallowed entries of the search space.
     * @return a collection of entries that will be ignored
     */
    public Collection<String> getBlacklist() {
        return blacklist;
    }

    /**
     * Dispatch this query and perform the corresponding search.
     * @return the result container
     */
    public MotifSearchResult run() {
        return motifSearchRuntime.performSearch(this);
    }

    /**
     * Dispatch this query and consume each accepted hit on-the-fly. This doesn't keep hits in memory.
     * @param hitConsumer terminal operation to perform on accepted hits
     */
    public void runAndConsume(Consumer<Hit> hitConsumer) {
        motifSearchRuntime.performSearch(this, hitConsumer);
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

    private String composeOutput(Hit hit, int scorePlaces, int matrixPlaces) {
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
}
