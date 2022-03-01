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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AssamSearchContext extends AbstractSearchContext<AssamSearchQuery, AssamParameters, AssamQueryStructure, AssamMotifSearchResult, AssamHit> {
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
        runtime.performSearch(this);
        return this.getResult();
    }

    /**
     * Dispatch this query and consume each accepted hit on-the-fly. This doesn't keep hits in memory.
     * @param hitConsumer terminal operation to perform on accepted hits
     */
    public void runAndConsume(Consumer<AssamHit> hitConsumer) {
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
    protected String composeOutput(AssamHit hit) {
        float[] original = hit.getTransformation().getFlattenedTransformation();
        List<Float> matrix = new ArrayList<>();
        for (float v : original) {
            matrix.add(truncate(v, config.getDecimalPlacesMatrix()));
        }

        return hit.getStructureIdentifier() + AbstractSearchContext.COLUMN_DELIMITER +
                hit.getAssemblyIdentifier() + AbstractSearchContext.COLUMN_DELIMITER +
                truncate(hit.getRootMeanSquareDeviation(), config.getDecimalPlacesScore()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.getLabelSelections()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(hit.getResidueTypes()) + AbstractSearchContext.COLUMN_DELIMITER +
                toString(matrix) + System.lineSeparator();
    }

    @Override
    public AssamMotifSearchResult getResult() {
        return result;
    }
}
