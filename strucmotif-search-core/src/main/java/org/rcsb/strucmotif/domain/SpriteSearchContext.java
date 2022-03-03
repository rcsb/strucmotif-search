package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.query.AssamParameters;
import org.rcsb.strucmotif.domain.query.AssamSearchQuery;
import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.query.SpriteParameters;
import org.rcsb.strucmotif.domain.query.SpriteQueryStructure;
import org.rcsb.strucmotif.domain.query.SpriteSearchQuery;
import org.rcsb.strucmotif.domain.query.StructureDeterminationMethodology;
import org.rcsb.strucmotif.domain.result.SpriteHit;
import org.rcsb.strucmotif.domain.result.SpriteMotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SpriteSearchContext extends AbstractSearchContext<SpriteSearchQuery, SpriteParameters, SpriteQueryStructure, SpriteMotifSearchResult, SpriteHit> {
    private static final Logger logger = LoggerFactory.getLogger(SpriteSearchContext.class);
    private final MotifSearchRuntime runtime;
    private final MotifSearchConfig config;
    private final InvertedIndex invertedIndex;
    private final StructureIndexProvider structureIndexProvider;
    private final StructureDataProvider structureDataProvider;
    private final SpriteSearchQuery query;
    private final SpriteMotifSearchResult result;

    public SpriteSearchContext(MotifSearchRuntime motifSearchRuntime, MotifSearchConfig config, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, SpriteSearchQuery query) {
        this.runtime = motifSearchRuntime;
        this.config = config;
        this.invertedIndex = invertedIndex;
        this.structureIndexProvider = structureIndexProvider;
        this.structureDataProvider = structureDataProvider;
        this.query = query;
        this.result = initializeResultContainer();
    }

    private SpriteMotifSearchResult initializeResultContainer() {
        SpriteQueryStructure queryStructure = query.getQueryStructure();
        SpriteParameters parameters = query.getParameters();
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
    public SpriteSearchQuery getQuery() {
        return query;
    }

    /**
     * Dispatch this query and perform the corresponding search.
     * @return the result container
     */
    public SpriteMotifSearchResult run() {
        runtime.performSearch(this);
        return this.getResult();
    }

    /**
     * Dispatch this query and consume each accepted hit on-the-fly. This doesn't keep hits in memory.
     * @param hitConsumer terminal operation to perform on accepted hits
     */
    public void runAndConsume(Consumer<SpriteHit> hitConsumer) {
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
    protected String composeOutput(SpriteHit hit) {
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
    public SpriteMotifSearchResult getResult() {
        return result;
    }

    public AssamSearchContext createSubcontext(EnrichedMotifDefinition motifDefinition) {
        SpriteParameters parentParameters = query.getParameters();
        AssamParameters parameters = new AssamParameters(parentParameters.getBackboneDistanceTolerance(),
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
        AssamSearchQuery query = new AssamSearchQuery(motifDefinition.getStructureIdentifier(),
                motifDefinition.getStructure(),
                motifDefinition.getLabelSelections(),
                motifDefinition.getResidues(),
                parameters,
                exchanges,
                Collections.emptySet(),
                Collections.emptySet(),
                StructureDeterminationMethodology.ALL,
                config);

        return new AssamSearchContext(runtime,
                config,
                invertedIndex,
                structureIndexProvider,
                structureDataProvider,
                query);
    }
}
