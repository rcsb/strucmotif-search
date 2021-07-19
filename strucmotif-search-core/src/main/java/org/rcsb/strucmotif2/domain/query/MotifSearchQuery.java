package org.rcsb.strucmotif2.domain.query;

import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.rcsb.strucmotif2.core.MotifSearchRuntime;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif2.domain.result.Hit;
import org.rcsb.strucmotif2.domain.result.MotifSearchResult;
import org.rcsb.strucmotif2.domain.result.SimpleHit;
import org.rcsb.strucmotif2.domain.result.TransformedHit;
import org.rcsb.strucmotif2.domain.selection.LabelSelection;
import org.rcsb.strucmotif2.domain.structure.ResidueType;
import org.rcsb.strucmotif2.domain.structure.Structure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
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
    private final Collection<StructureIdentifier> whitelist;
    private final Collection<StructureIdentifier> blacklist;
    private final MotifSearchConfig config;

    MotifSearchQuery(MotifSearchRuntime motifSearchRuntime,
                     Structure structure,
                     Parameters parameters,
                     Map<LabelSelection, Set<ResidueType>> exchanges,
                     Collection<StructureIdentifier> whitelist,
                     Collection<StructureIdentifier> blacklist,
                     MotifSearchConfig config) {
        this.motifSearchRuntime = motifSearchRuntime;
        List<ResiduePairOccurrence> residuePairOccurrences = parameters.getMotifPruner().prune(structure);
        this.queryStructure = new QueryStructure(structure, residuePairOccurrences);
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
    public Collection<StructureIdentifier> getWhitelist() {
        return whitelist;
    }

    /**
     * Returns disallowed entries of the search space.
     * @return a collection of entries that will be ignored
     */
    public Collection<StructureIdentifier> getBlacklist() {
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
        // transformed/complex hits will be written
        if (this.parameters.getScoringStrategy().equals(ScoringStrategy.ALIGNMENT)) {
            return "pdb_id" + COLUMN_DELIMITER +
                    "assembly_id" + COLUMN_DELIMITER +
                    "rmsd_score" + COLUMN_DELIMITER +
                    "geom_score" + COLUMN_DELIMITER +
                    "residue_ids" + COLUMN_DELIMITER +
                    "residue_types" + COLUMN_DELIMITER +
                    "transformation" + System.lineSeparator();
        }

        // simple hits will be written
        return "pdb_id" + COLUMN_DELIMITER +
                "assembly_id" + COLUMN_DELIMITER +
                "geom_score" + COLUMN_DELIMITER +
                "residue_ids" + System.lineSeparator();
    }

    private String composeOutput(Hit hit, int scorePlaces, int matrixPlaces) {
        if (hit instanceof TransformedHit) {
            TransformedHit transformedHit = (TransformedHit) hit;
            return transformedHit.getStructureIdentifier() + COLUMN_DELIMITER +
                    transformedHit.getAssemblyIdentifier() + COLUMN_DELIMITER +
                    truncate(transformedHit.getRootMeanSquareDeviation().value(), scorePlaces) + COLUMN_DELIMITER +
                    truncate(transformedHit.getGeometricDescriptorScore().value(), scorePlaces) + COLUMN_DELIMITER +
                    toString(transformedHit.getSelection()) + COLUMN_DELIMITER +
                    toString(transformedHit.getResidueTypes()) + COLUMN_DELIMITER +
                    toString(Arrays.stream(transformedHit.getTransformation().getFlattenedTransformation()).mapToObj(d -> truncate(d, matrixPlaces)).collect(Collectors.toList())) + System.lineSeparator();
        }

        if (hit instanceof SimpleHit) {
            return hit.getStructureIdentifier() + COLUMN_DELIMITER +
                    hit.getAssemblyIdentifier() + COLUMN_DELIMITER +
                    truncate(hit.getGeometricDescriptorScore().value(), scorePlaces) + COLUMN_DELIMITER +
                    toString(hit.getSelection()) + System.lineSeparator();
        }

        throw new UnsupportedOperationException("No impl to handle " + hit.getClass().getSimpleName());
    }

    private String toString(Collection<?> collection) {
        return collection.stream().map(Object::toString).collect(Collectors.joining(VALUE_DELIMITER, "[", "]"));
    }

    /**
     * Truncate doubles to any number of decimal places.
     * @param value the raw value
     * @return a double with the specified number of decimal places
     */
    private double truncate(double value, int decimalPlaces) {
        return BigDecimal.valueOf(value)
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
