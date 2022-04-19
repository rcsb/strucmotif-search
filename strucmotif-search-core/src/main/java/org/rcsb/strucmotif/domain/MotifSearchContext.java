package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.StrucmotifRuntime;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.query.StructureParameters;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.query.MotifParameters;
import org.rcsb.strucmotif.domain.query.MotifQueryStructure;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.ContentType;
import org.rcsb.strucmotif.domain.result.MotifHit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The context when in 'detect-motif' mode.
 */
public class MotifSearchContext extends AbstractSearchContext<MotifParameters, MotifQueryStructure, MotifHit> {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchContext.class);
    private static final Collection<ContentType> DEFAULT_CONTENT_TYPES = EnumSet.of(ContentType.EXPERIMENTAL, ContentType.COMPUTATIONAL);
    private final StrucmotifRuntime runtime;
    private final StrucmotifConfig config;
    private final InvertedIndex invertedIndex;
    private final StructureIndexProvider structureIndexProvider;
    private final StructureDataProvider structureDataProvider;
    private final MotifSearchQuery query;
    private final MotifSearchResult result;

    /**
     * Create a context.
     * @param strucmotifRuntime runtime
     * @param strucmotifConfig config
     * @param invertedIndex index
     * @param structureIndexProvider index provider
     * @param structureDataProvider data provider
     * @param query the actual query
     */
    public MotifSearchContext(StrucmotifRuntime strucmotifRuntime, StrucmotifConfig strucmotifConfig, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, MotifSearchQuery query) {
        this.runtime = strucmotifRuntime;
        this.config = strucmotifConfig;
        this.invertedIndex = invertedIndex;
        this.structureIndexProvider = structureIndexProvider;
        this.structureDataProvider = structureDataProvider;
        this.query = query;
        this.result = initializeResultContainer();
    }

    private MotifSearchResult initializeResultContainer() {
        MotifQueryStructure queryStructure = query.getQueryStructure();
        MotifParameters parameters = query.getParameters();
        logger.info("[{}] Query: {}",
                id,
                queryStructure.getStructureIdentifier());
        logger.info("[{}] Tolerances: [{}, {}, {}], Atom Pairing Scheme: {}, RMSD Cutoff: {}",
                id,
                parameters.getBackboneDistanceTolerance(),
                parameters.getSideChainDistanceTolerance(),
                parameters.getAngleTolerance(),
                parameters.getAtomPairingScheme(),
                parameters.getRmsdCutoff());

        return new MotifSearchResult();
    }

    @Override
    public StrucmotifRuntime getRuntime() {
        return runtime;
    }

    @Override
    public StrucmotifConfig getConfig() {
        return config;
    }

    @Override
    public InvertedIndex getInvertedIndex() {
        return invertedIndex;
    }

    @Override
    public StructureIndexProvider getStructureIndexProvider() {
        return structureIndexProvider;
    }

    @Override
    public StructureDataProvider getStructureDataProvider() {
        return structureDataProvider;
    }

    @Override
    public MotifSearchQuery getQuery() {
        return query;
    }

    @Override
    public MotifSearchResult run() {
        runtime.performSearch(this);
        return this.getResult();
    }

    @Override
    public void runAndConsume(Consumer<MotifHit> hitConsumer) {
        getRuntime().performSearch(this, hitConsumer);
    }

    @Override
    protected String composeHeader() {
        return "motif_id" + AbstractSearchContext.COLUMN_DELIMITER +
                "rmsd_score" + AbstractSearchContext.COLUMN_DELIMITER +
                "residue_ids" + AbstractSearchContext.COLUMN_DELIMITER +
                "residue_types" + AbstractSearchContext.COLUMN_DELIMITER +
                "transformation" + System.lineSeparator();
    }

    @Override
    protected String composeOutput(MotifHit hit) {
        float[] original = hit.getTransformation().getFlattenedTransformation();
        List<Float> matrix = new ArrayList<>();
        for (float v : original) {
            matrix.add(truncate(v, config.getDecimalPlacesMatrix()));
        }

        return hit.getMotifIdentifier() + AbstractSearchContext.COLUMN_DELIMITER +
                truncate(hit.getRootMeanSquareDeviation(), config.getDecimalPlacesScore()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.getLabelSelections()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.getResidueTypes()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(matrix) + System.lineSeparator();
    }

    @Override
    public MotifSearchResult getResult() {
        return result;
    }

    /**
     * Create a subcontext to detect individual motifs in the referenced structure.
     * @param motifDefinition the motif to screen for
     * @return a child context
     */
    public StructureSearchContext createSubcontext(EnrichedMotifDefinition motifDefinition) {
        MotifParameters parentParameters = query.getParameters();
        StructureParameters parameters = new StructureParameters(parentParameters.getBackboneDistanceTolerance(),
                parentParameters.getSideChainDistanceTolerance(),
                parentParameters.getAngleTolerance(),
                parentParameters.getRmsdCutoff(),
                parentParameters.getAtomPairingScheme(),
                parentParameters.getMotifPruner(),
                Integer.MAX_VALUE,
                false);

        Map<LabelSelection, Set<ResidueType>> exchanges = motifDefinition.getPositionSpecificExchanges()
                .stream()
                .collect(Collectors.toMap(PositionSpecificExchange::getLabelSelection, PositionSpecificExchange::getResidueTypes));
        StructureQuery query = new StructureQuery(motifDefinition.getStructureIdentifier(),
                motifDefinition.getStructure(),
                motifDefinition.getLabelSelections(),
                motifDefinition.getResidues(),
                parameters,
                exchanges,
                Collections.emptySet(),
                Collections.emptySet(),
                DEFAULT_CONTENT_TYPES,
                config);

        return new StructureSearchContext(runtime,
                config,
                invertedIndex,
                structureIndexProvider,
                structureDataProvider,
                query);
    }
}
