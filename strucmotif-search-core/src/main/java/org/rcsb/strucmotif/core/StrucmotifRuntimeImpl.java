package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.StructureSearchContext;
import org.rcsb.strucmotif.domain.MotifSearchContext;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.query.StructureParameters;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.result.StructureHit;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
import org.rcsb.strucmotif.domain.result.MotifHit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class StrucmotifRuntimeImpl implements StrucmotifRuntime {
    private static final Logger logger = LoggerFactory.getLogger(StrucmotifRuntimeImpl.class);
    private final TargetAssembler targetAssembler;
    private final ThreadPool threadPool;
    private final StrucmotifConfig strucmotifConfig;
    private final AlignmentService alignmentService;
    private final AssemblyInformationProvider assemblyInformationProvider;

    /**
     * Injectable constructor.
     * @param targetAssembler target assembler
     * @param threadPool thread pool
     * @param strucmotifConfig app config
     * @param alignmentService alignment service
     * @param assemblyInformationProvider all known assemblies
     */
    @Autowired
    public StrucmotifRuntimeImpl(TargetAssembler targetAssembler, ThreadPool threadPool, StrucmotifConfig strucmotifConfig, AlignmentService alignmentService, AssemblyInformationProvider assemblyInformationProvider) {
        this.targetAssembler = targetAssembler;
        this.threadPool = threadPool;
        this.strucmotifConfig = strucmotifConfig;
        this.alignmentService = alignmentService;
        this.assemblyInformationProvider = assemblyInformationProvider;
    }

    @Override
    public void performSearch(StructureSearchContext context) {
        try {
            StructureSearchResult result = context.getResult();

            // get all valid targets
            targetAssembler.assemble(context);

            List<StructureHit> hits = scoreHits(context);
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
    public void performSearch(StructureSearchContext context, Consumer<StructureHit> consumer) {
        try {
            StructureSearchResult result = context.getResult();

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

    private List<StructureHit> scoreHits(StructureSearchContext context) throws ExecutionException, InterruptedException {
        StructureQuery query = context.getQuery();
        StructureParameters parameters = query.getParameters();
        StructureSearchResult result = context.getResult();

        result.getTimings().scoreHitsStart();
        int limit = Math.min(parameters.getLimit(), strucmotifConfig.getMaxResults());
        HitScorer hitScorer = new HitScorer(query.getQueryStructure().getResidues(),
                parameters.getAtomPairingScheme(),
                alignmentService);

        List<StructureHit> hits = threadPool.submit(() -> hits(context, hitScorer)
                .limit(limit)
                .collect(Collectors.toList())).get();

        result.getTimings().scoreHitsStop();
        return hits;
    }

    private int consumeHits(StructureSearchContext context, Consumer<StructureHit> consumer) throws ExecutionException, InterruptedException {
        StructureQuery query = context.getQuery();
        StructureParameters parameters = query.getParameters();
        StructureSearchResult result = context.getResult();

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

    private Stream<StructureHit> hits(StructureSearchContext context, HitScorer hitScorer) {
        StructureQuery query = context.getQuery();
        List<Integer> residueIndexSwaps = query.getQueryStructure().getResidueIndexSwaps();
        StructureParameters parameters = query.getParameters();
        StructureIndexProvider structureIndexProvider = context.getStructureIndexProvider();
        StructureDataProvider structureDataProvider = context.getStructureDataProvider();

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
    public void performSearch(MotifSearchContext context) {
        try {
            MotifSearchResult result = context.getResult();
            List<MotifHit> hits = threadPool.submit(() -> context.getQuery()
                    .getMotifDefinitions()
                    .parallelStream()
                    .flatMap(motif -> {
                        StructureSearchResult subresult = performSearch(context, motif);
                        List<StructureHit> subhits = subresult.getHits();
                        if (subhits.isEmpty()) return Stream.empty();

                        logger.info("[{}] {} occurrences of {} found", context.getId(), subhits.size(), motif.getMotifIdentifier());
                        // if there are hits: move them to parent
                        return subresult.getHits()
                                .stream()
                                .map(h -> createSubhit(motif, h));
                    })
                    .collect(Collectors.toList())).get();
            result.setHits(hits);
            result.getTimings().queryStop();

            logger.info("[{}] Accepted {} hits in {} ms",
                    context.getId(),
                    hits.size(),
                    result.getTimings().getQueryTime());
        } catch (Exception e) {
            // unwrap specific exceptions
            Throwable t = unwrapException(e);
            if (t instanceof IllegalQueryDefinitionException) {
                throw (IllegalQueryDefinitionException) t;
            }
            throw new RuntimeException(e);
        }
    }

    private StructureSearchResult performSearch(MotifSearchContext context, EnrichedMotifDefinition motifDefinition) {
        StructureSearchContext subcontext = context.createSubcontext(motifDefinition);
        logger.info("[{}] Evaluating {} in subquery [{}]", context.getId(), motifDefinition.getMotifIdentifier(), subcontext.getId());

        // delegate to traditional route
        performSearch(subcontext);

        return subcontext.getResult();
    }

    @Override
    public void performSearch(MotifSearchContext context, Consumer<MotifHit> consumer) {
        try {
            MotifSearchResult result = context.getResult();

            int hits = consumeHits(context, consumer);

            logger.info("[{}] Accepted {} hits in {} ms",
                    context.getId(),
                    hits,
                    result.getTimings().getQueryTime());
        } catch (Exception e) {
            // unwrap specific exceptions
            Throwable t = unwrapException(e);
            if (t instanceof IllegalQueryDefinitionException) {
                throw (IllegalQueryDefinitionException) t;
            }
            throw new RuntimeException(e);
        }
    }

    private int consumeHits(MotifSearchContext context, Consumer<MotifHit> consumer) throws ExecutionException, InterruptedException {
        MotifSearchResult result = context.getResult();
        AtomicInteger hits = new AtomicInteger();

        threadPool.submit(() -> {
            context.getQuery()
                    .getMotifDefinitions()
                    .parallelStream()
                    .forEach(motif -> {
                        StructureSearchResult subresult = performSearch(context, motif);
                        List<StructureHit> subhits = subresult.getHits();
                        if (subhits.isEmpty()) return;

                        int subhitCount = subhits.size();
                        logger.info("[{}] {} occurrences of {} found", context.getId(), subhitCount, motif.getMotifIdentifier());
                        hits.addAndGet(subhitCount);
                        // if there are hits: move them to parent
                        subresult.getHits()
                                .stream()
                                .map(h -> createSubhit(motif, h))
                                .forEach(consumer);
                    });
            return null;
        }).get();

        result.getTimings().queryStop();
        return hits.get();
    }

    private MotifHit createSubhit(MotifDefinition motifDefinition, StructureHit structureHit) {
        return new MotifHit(motifDefinition.getMotifIdentifier(),
                structureHit.getLabelSelections(),
                structureHit.getResidueTypes(),
                structureHit.getRootMeanSquareDeviation(),
                structureHit.getTransformation());
    }
}
