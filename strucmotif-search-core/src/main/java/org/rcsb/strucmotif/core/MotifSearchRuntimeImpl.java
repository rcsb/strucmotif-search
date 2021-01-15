package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.query.ScoringStrategy;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class MotifSearchRuntimeImpl implements MotifSearchRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchRuntimeImpl.class);
    private final TargetAssembler targetAssembler;
    private final ThreadPool threadPool;
    private final MotifSearchConfig motifSearchConfig;
    private final AlignmentService alignmentService;
    private final StructureDataProvider structureDataProvider;
    private final StateRepository stateRepository;

    @Autowired
    public MotifSearchRuntimeImpl(TargetAssembler targetAssembler, ThreadPool threadPool, MotifSearchConfig motifSearchConfig, AlignmentService alignmentService, StructureDataProvider structureDataProvider, StateRepository stateRepository) {
        this.targetAssembler = targetAssembler;
        this.threadPool = threadPool;
        this.motifSearchConfig = motifSearchConfig;
        this.alignmentService = alignmentService;
        this.structureDataProvider = structureDataProvider;
        this.stateRepository = stateRepository;
    }

    @Override
    public MotifSearchResult performSearch(MotifSearchQuery query) {
        try {
            QueryStructure queryStructure = query.getQueryStructure();

            // all motifs which can be formed from this query
            List<ResiduePairDescriptor> queryResiduePairDescriptors = queryStructure.getResiduePairDescriptors();

            Parameters parameters = query.getParameters();
            logger.info("[{}] Query: {}, Exchanges: {}, Tolerances: [{}, {}, {}], Cutoff: {}",
                    query.hashCode(),
                    queryResiduePairDescriptors,
                    query.getExchanges(),
                    parameters.getBackboneDistanceTolerance(),
                    parameters.getSideChainDistanceTolerance(),
                    parameters.getAngleTolerance(),
                    parameters.getScoringStrategy() == ScoringStrategy.DESCRIPTOR ? parameters.getScoreCutoff() : parameters.getRmsdCutoff());

            MotifSearchResult result = new MotifSearchResult(query);

            // get all valid targets
            targetAssembler.assemble(result);

            List<? extends Hit> hits = scoreHits(parameters, result);
            logger.info("[{}] Accepted {} hits in {} ms",
                    query.hashCode(),
                    hits.size(),
                    result.getTimings().getScoreHitsTime());

            // dereference target structure map
            result.getTargetStructures().clear();
            result.setTargetStructures(null);

            result.setHits(hits);
            result.getTimings().queryStop();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<? extends Hit> scoreHits(Parameters parameters, MotifSearchResult result) throws ExecutionException, InterruptedException {
        result.getTimings().scoreHitsStart();
        int limit = Math.min(parameters.getLimit(), motifSearchConfig.getMaxResults());
        List<? extends Hit> hits;
        switch (parameters.getScoringStrategy()) {
            case ALIGNMENT:
                HitScorer hitScorer = new RootMeanSquareDeviationHitScorer(result.getQuery().getQueryStructure().getStructure(),
                        parameters.getAtomPairingScheme(), alignmentService, structureDataProvider);
                hits = threadPool.submit(() -> result.getTargetStructures()
                        .values()
                        .parallelStream()
                        .flatMap(targetStructure -> targetStructure.paths(stateRepository))
                        // filter hits if desired
                        .filter(simpleHit -> simpleHit.getGeometricDescriptorScore().value() >= parameters.getScoreCutoff())
                        // align
                        .map(hitScorer::score)
                        .filter(transformedHit -> transformedHit.getRootMeanSquareDeviation().value() <= parameters.getRmsdCutoff())
                        .limit(limit)
                        .collect(Collectors.toList()))
                        .get();
                break;
            case DESCRIPTOR:
                hits = threadPool.submit(() -> result.getTargetStructures()
                        .values()
                        .parallelStream()
                        .flatMap(targetStructure -> targetStructure.paths(stateRepository))
                        // filter hits if desired
                        .filter(simpleHit -> simpleHit.getGeometricDescriptorScore().value() >= parameters.getScoreCutoff())
                        .limit(limit)
                        .collect(Collectors.toList()))
                        .get();
                break;
            default:
                throw new IllegalArgumentException("Unknown scoring strategy: " + parameters.getScoringStrategy());
        }
        result.getTimings().scoreHitsStop();
        return hits;
    }
}
