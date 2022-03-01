package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.AssamSearchContext;
import org.rcsb.strucmotif.domain.SpriteSearchContext;
import org.rcsb.strucmotif.domain.query.AssamParameters;
import org.rcsb.strucmotif.domain.query.AssamSearchQuery;
import org.rcsb.strucmotif.domain.result.AssamHit;
import org.rcsb.strucmotif.domain.result.AssamMotifSearchResult;
import org.rcsb.strucmotif.domain.result.SpriteHit;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
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
    private final MotifDefinitionRegistry motifDefinitionRegistry;

    /**
     * Injectable constructor.
     * @param targetAssembler target assembler
     * @param threadPool thread pool
     * @param motifSearchConfig app config
     * @param alignmentService alignment service
     * @param structureDataProvider structure data provider
     * @param structureIndexProvider maps from index to identifier
     * @param assemblyInformationProvider all known assemblies
     * @param motifDefinitionRegistry all known motifs
     */
    @Autowired
    public MotifSearchRuntimeImpl(TargetAssembler targetAssembler, ThreadPool threadPool, MotifSearchConfig motifSearchConfig, AlignmentService alignmentService, StructureDataProvider structureDataProvider, StructureIndexProvider structureIndexProvider, AssemblyInformationProvider assemblyInformationProvider, MotifDefinitionRegistry motifDefinitionRegistry) {
        this.targetAssembler = targetAssembler;
        this.threadPool = threadPool;
        this.motifSearchConfig = motifSearchConfig;
        this.alignmentService = alignmentService;
        this.structureDataProvider = structureDataProvider;
        this.structureIndexProvider = structureIndexProvider;
        this.assemblyInformationProvider = assemblyInformationProvider;
        this.motifDefinitionRegistry = motifDefinitionRegistry;

        // initialize structure cache (if active)
        try {
            structureDataProvider.initializeRenumberedStructureCache();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void performSearch(AssamSearchContext context) {
        try {
            AssamMotifSearchResult result = context.getResult();

            // get all valid targets
            targetAssembler.assemble(context);

            List<AssamHit> hits = scoreHits(context);
            logger.info("[{}] Accepted {} hits in {} ms",
                    context.getId(),
                    hits.size(),
                    result.getTimings().getScoreHitsTime());

            // dereference target structure map
            result.getTargetStructures().clear();
            result.setTargetStructures(null);

            result.setHits(hits);
            result.getTimings().queryStop();
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
    public void performSearch(AssamSearchContext context, Consumer<AssamHit> consumer) {
        try {
            AssamSearchQuery query = context.getQuery();
            AssamMotifSearchResult result = context.getResult();

            // get all valid targets
            targetAssembler.assemble(context);

            int hits = consumeHits(context, consumer);
            logger.info("[{}] Accepted {} hits in {} ms",
                    context.getId(),
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

    private static Throwable unwrapException(Throwable throwable) {
        Objects.requireNonNull(throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private List<AssamHit> scoreHits(AssamSearchContext context) throws ExecutionException, InterruptedException {
        AssamSearchQuery query = context.getQuery();
        AssamParameters parameters = query.getParameters();
        AssamMotifSearchResult result = context.getResult();

        result.getTimings().scoreHitsStart();
        int limit = Math.min(parameters.getLimit(), motifSearchConfig.getMaxResults());
        HitScorer hitScorer = new HitScorer(query.getQueryStructure().getResidues(),
                parameters.getAtomPairingScheme(),
                alignmentService);

        List<AssamHit> hits = threadPool.submit(() -> hits(context, hitScorer)
                .limit(limit)
                .collect(Collectors.toList())).get();

        result.getTimings().scoreHitsStop();
        return hits;
    }

    private int consumeHits(AssamSearchContext context, Consumer<AssamHit> consumer) throws ExecutionException, InterruptedException {
        AssamSearchQuery query = context.getQuery();
        AssamParameters parameters = query.getParameters();
        AssamMotifSearchResult result = context.getResult();

        result.getTimings().scoreHitsStart();
        AtomicInteger hits = new AtomicInteger();
        HitScorer hitScorer = new HitScorer(query.getQueryStructure().getResidues(),
                parameters.getAtomPairingScheme(),
                alignmentService);

        threadPool.submit(() -> {
            hits(context, hitScorer)
                    .forEach(hit -> {
                        hits.incrementAndGet();
                        consumer.accept(hit);
                    });
            return null;
        }).get();

        result.getTimings().scoreHitsStop();
        return hits.get();
    }

    private Stream<AssamHit> hits(AssamSearchContext context, HitScorer hitScorer) {
        AssamSearchQuery query = context.getQuery();
        List<Integer> residueIndexSwaps = query.getQueryStructure().getResidueIndexSwaps();
        AssamParameters parameters = query.getParameters();

        return context.getResult()
                .getTargetStructures()
                .values()
                .parallelStream()
                .flatMap(targetStructure -> {
                    String structureIdentifier = structureIndexProvider.selectStructureIdentifier(targetStructure.getStructureIndex());
                    Structure structure = structureDataProvider.readRenumbered(structureIdentifier);
                    return targetStructure.paths(residueIndexSwaps, structure, structureIdentifier, hitScorer, parameters.getRmsdCutoff(), assemblyInformationProvider, parameters.isUndefinedAssemblies());
                });
    }

    @Override
    public void performSearch(SpriteSearchContext context) {
        ResidueGraph residueGraph = context.getQuery().getQueryStructure().getResidueGraph();
        logger.info("[{}] Extracted {} residue pairs",
                context.getId(),
                residueGraph.getNumberOfPairings());


    }

    @Override
    public void performSearch(SpriteSearchContext context, Consumer<SpriteHit> consumer) {
        // TODO impl
    }
}
