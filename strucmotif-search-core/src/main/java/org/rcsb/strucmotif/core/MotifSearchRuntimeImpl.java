package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default strucmotif-search runtime.
 */
@Service
public class MotifSearchRuntimeImpl implements MotifSearchRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearchRuntimeImpl.class);
    private final TargetAssembler targetAssembler;
    private final ThreadPool threadPool;
    private final MotifSearchConfig motifSearchConfig;
    private final AlignmentService alignmentService;
    private final StructureDataProvider structureDataProvider;
    private final StructureIndexProvider structureIndexProvider;
    private final AssemblyInformationProvider assemblyInformationProvider;

    /**
     * Injectable constructor.
     * @param targetAssembler target assembler
     * @param threadPool thread pool
     * @param motifSearchConfig app config
     * @param alignmentService alignment service
     * @param structureDataProvider structure data provider
     * @param structureIndexProvider maps from index to identifier
     * @param assemblyInformationProvider all known assemblies
     */
    @Autowired
    public MotifSearchRuntimeImpl(TargetAssembler targetAssembler, ThreadPool threadPool, MotifSearchConfig motifSearchConfig, AlignmentService alignmentService, StructureDataProvider structureDataProvider, StructureIndexProvider structureIndexProvider, AssemblyInformationProvider assemblyInformationProvider) {
        this.targetAssembler = targetAssembler;
        this.threadPool = threadPool;
        this.motifSearchConfig = motifSearchConfig;
        this.alignmentService = alignmentService;
        this.structureDataProvider = structureDataProvider;
        this.structureIndexProvider = structureIndexProvider;
        this.assemblyInformationProvider = assemblyInformationProvider;

        // initialize structure cache (if active)
        try {
            structureDataProvider.initializeRenumberedStructureCache();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public MotifSearchResult performSearch(MotifSearchQuery query) {
        try {
            QueryStructure queryStructure = query.getQueryStructure();
            Parameters parameters = query.getParameters();
            MotifSearchResult result = createResultContainer(query, queryStructure, parameters);

            // get all valid targets
            targetAssembler.assemble(result);

            List<Hit> hits = scoreHits(parameters, result, queryStructure.getResidueIndexSwaps());
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
            // unwrap specific exceptions
            Throwable t = unwrapException(e);
            if (t instanceof IllegalQueryDefinitionException) {
                throw (IllegalQueryDefinitionException) t;
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void performSearch(MotifSearchQuery query, Consumer<Hit> consumer) {
        try {
            QueryStructure queryStructure = query.getQueryStructure();
            Parameters parameters = query.getParameters();
            MotifSearchResult result = createResultContainer(query, queryStructure, parameters);

            // get all valid targets
            targetAssembler.assemble(result);

            int hits = consumeHits(parameters, result, consumer, queryStructure.getResidueIndexSwaps());
            logger.info("[{}] Accepted {} hits in {} ms",
                    query.hashCode(),
                    hits,
                    result.getTimings().getScoreHitsTime());
        } catch (Exception e) {
            // unwrap specific exceptions
            Throwable t = unwrapException(e);
            if (t instanceof IllegalQueryDefinitionException) {
                throw (IllegalQueryDefinitionException) t;
            }
            throw new RuntimeException(e);
        }
    }

    private MotifSearchResult createResultContainer(MotifSearchQuery query, QueryStructure queryStructure, Parameters parameters) {
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

        return new MotifSearchResult(query);
    }

    private static Throwable unwrapException(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private List<Hit> scoreHits(Parameters parameters, MotifSearchResult result, List<Integer> residueIndexSwaps) throws ExecutionException, InterruptedException {
        result.getTimings().scoreHitsStart();
        int limit = Math.min(parameters.getLimit(), motifSearchConfig.getMaxResults());
        HitScorer hitScorer = new HitScorer(result.getQuery().getQueryStructure().getResidues(),
                parameters.getAtomPairingScheme(),
                alignmentService);

        List<Hit> hits = threadPool.submit(() -> hits(result, parameters, hitScorer, residueIndexSwaps)
                .limit(limit)
                .collect(Collectors.toList())).get();

        result.getTimings().scoreHitsStop();
        return hits;
    }

    private int consumeHits(Parameters parameters, MotifSearchResult result, Consumer<Hit> consumer, List<Integer> residueIndexSwaps) throws ExecutionException, InterruptedException {
        result.getTimings().scoreHitsStart();
        AtomicInteger hits = new AtomicInteger();
        HitScorer hitScorer = new HitScorer(result.getQuery().getQueryStructure().getResidues(),
                parameters.getAtomPairingScheme(),
                alignmentService);

        threadPool.submit(() -> {
            hits(result, parameters, hitScorer, residueIndexSwaps)
                    .forEach(hit -> {
                        hits.incrementAndGet();
                        consumer.accept(hit);
                    });
            return null;
        }).get();

        result.getTimings().scoreHitsStop();
        return hits.get();
    }

    private Stream<Hit> hits(MotifSearchResult result, Parameters parameters, HitScorer hitScorer, List<Integer> residueIndexSwaps) {
        return result.getTargetStructures()
                .values()
                .parallelStream()
                .flatMap(targetStructure -> {
                    String structureIdentifier = structureIndexProvider.selectStructureIdentifier(targetStructure.getStructureIndex());
                    Structure structure = structureDataProvider.readRenumbered(structureIdentifier);
                    return targetStructure.paths(residueIndexSwaps, structure, structureIdentifier, hitScorer, parameters.getRmsdCutoff(), assemblyInformationProvider, parameters.isUndefinedAssemblies());
                });
    }
}
