package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.StrucmotifRuntime;
import org.rcsb.strucmotif.domain.query.StructureParameters;
import org.rcsb.strucmotif.domain.query.StructureQueryStructure;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.result.StructureHit;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The context when in 'search-for-structures' mode.
 */
public class StructureSearchContext extends AbstractSearchContext<StructureParameters, StructureQueryStructure, StructureHit> {
    private static final Logger logger = LoggerFactory.getLogger(StructureSearchContext.class);
    private final StrucmotifRuntime runtime;
    private final StrucmotifConfig config;
    private final InvertedIndex invertedIndex;
    private final StructureIndexProvider structureIndexProvider;
    private final StructureDataProvider structureDataProvider;
    private final StructureQuery query;
    private final StructureSearchResult result;

    /**
     * Create a context.
     * @param strucmotifRuntime runtime
     * @param strucmotifConfig config
     * @param invertedIndex index
     * @param structureIndexProvider index provider
     * @param structureDataProvider data provider
     * @param query the actual query
     */
    public StructureSearchContext(StrucmotifRuntime strucmotifRuntime, StrucmotifConfig strucmotifConfig, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, StructureQuery query) {
        this.runtime = strucmotifRuntime;
        this.config = strucmotifConfig;
        this.invertedIndex = invertedIndex;
        this.structureIndexProvider = structureIndexProvider;
        this.structureDataProvider = structureDataProvider;
        this.query = query;
        this.result = initializeResultContainer();
    }

    private StructureSearchResult initializeResultContainer() {
        StructureQueryStructure queryStructure = query.getQueryStructure();
        StructureParameters parameters = query.getParameters();
        logger.info("[{}] Query: {} with {}",
                id,
                queryStructure.getStructureIdentifier(),
                queryStructure.getResiduePairOccurrences());
        logger.info("[{}] Exchanges: {}, Tolerances: [{}, {}, {}], Atom Pairing Scheme: {}, RMSD Cutoff: {}, Limit: {}",
                id,
                query.getExchanges(),
                parameters.getBackboneDistanceTolerance(),
                parameters.getSideChainDistanceTolerance(),
                parameters.getAngleTolerance(),
                parameters.getAtomPairingScheme(),
                parameters.getRmsdCutoff(),
                parameters.getLimit());

        return new StructureSearchResult();
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
    public StructureQuery getQuery() {
        return query;
    }

    @Override
    public StructureSearchResult run() {
        runtime.performSearch(this);
        return this.getResult();
    }

    @Override
    public void runAndConsume(Consumer<StructureHit> hitConsumer) {
        getRuntime().performSearch(this, hitConsumer);
    }

    @Override
    protected String composeHeader() {
        return "pdb_id" + AbstractSearchContext.COLUMN_DELIMITER +
                "assembly_id" + AbstractSearchContext.COLUMN_DELIMITER +
                "rmsd_score" + AbstractSearchContext.COLUMN_DELIMITER +
                "residue_ids" + AbstractSearchContext.COLUMN_DELIMITER +
                "residue_types" + AbstractSearchContext.COLUMN_DELIMITER +
                "transformation" + System.lineSeparator();
    }

    @Override
    protected String composeOutput(StructureHit hit) {
        float[] original = hit.transformation();
        List<Float> matrix = new ArrayList<>();
        for (float v : original) {
            matrix.add(truncate(v, config.getDecimalPlacesMatrix()));
        }

        return hit.structureIdentifier() + AbstractSearchContext.COLUMN_DELIMITER +
                hit.assemblyIdentifier() + AbstractSearchContext.COLUMN_DELIMITER +
                truncate(hit.rootMeanSquareDeviation(), config.getDecimalPlacesScore()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.labelSelections()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.residueTypes()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(matrix) + System.lineSeparator();
    }

    @Override
    public StructureSearchResult getResult() {
        return result;
    }
}
