package org.rcsb.strucmotif.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.align.Alignment;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class InternalMotifSearchImpl implements InternalMotifSearch {
    private static final Logger logger = LoggerFactory.getLogger(InternalMotifSearchImpl.class);
    private TargetAssembler targetAssembler;
    private Alignment alignment;

    @Inject
    public InternalMotifSearchImpl(TargetAssembler targetAssembler, Alignment alignment) {
        this.targetAssembler = targetAssembler;
        this.alignment = alignment;
    }

    @Override
    public MotifSearchResult performSearch(MotifSearchQuery query) {
        try {
            QueryStructure queryStructure = query.getQueryStructure();

            // all motifs which can be formed from this query
            List<ResiduePairDescriptor> queryResiduePairDescriptors = queryStructure.getResiduePairDescriptors();

            if (queryResiduePairDescriptors.isEmpty()) {
                throw new IllegalArgumentException("did not find any valid motifs in " + queryStructure.getStructure().getStructureIdentifier() +
                        " - maybe distance cutoff (" + MotifSearch.DISTANCE_CUTOFF + ") exceeded? - maybe wrong selection?");
            }

            Parameters parameters = query.getParameters();
            logger.info("Query: {}, tolerances: [{}, {}, {}], exchanges: {}",
                    queryResiduePairDescriptors,
                    parameters.getBackboneDistanceTolerance(),
                    parameters.getSideChainDistanceTolerance(),
                    parameters.getAngleTolerance(),
                    query.getExchanges());

            MotifSearchResult result = new MotifSearchResult(query);

            // get all valid targets
            targetAssembler.assemble(result);

            HitScorer hitScorer = new HitScorerImpl(queryStructure, parameters.getRmsdCutoff(), parameters.getAtomPairingScheme(), alignment);
            List<Hit> hits = scoreHits(parameters, result, hitScorer);
            logger.info("Accepted {} hits in {} ms",
                    hits.size(),
                    result.getTimings().getStructuresTime());

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

    private List<Hit> scoreHits(Parameters parameters, MotifSearchResult result, HitScorer hitScorer) throws ExecutionException, InterruptedException {
        boolean honorLimit = parameters.hasLimit();
        result.getTimings().structuresStart();
        List<Hit> hits = honorLimit ? scoreHitsLimited(parameters, result, hitScorer) : scoreHitsUnlimited(result, hitScorer);
        result.getTimings().structuresStop();
        return hits;
    }

    private List<Hit> scoreHitsLimited(Parameters parameters, MotifSearchResult result, HitScorer hitScorer) throws ExecutionException, InterruptedException {
        return MotifSearch.FORK_JOIN_POOL.submit(() -> result.getTargetStructures()
                .values()
                .parallelStream()
                .flatMap(targetStructure -> targetStructure.paths().map(path -> hitScorer.score(targetStructure, path)))
                // hits filtered by high RMSD value are reported as null
                .filter(Objects::nonNull)
                .limit(parameters.getLimit())
                .collect(Collectors.toList()))
                .get();
    }

    private List<Hit> scoreHitsUnlimited(MotifSearchResult result, HitScorer hitScorer) throws ExecutionException, InterruptedException {
        return MotifSearch.FORK_JOIN_POOL.submit(() -> result.getTargetStructures()
                .values()
                .parallelStream()
                .flatMap(targetStructure -> targetStructure.paths().map(path -> hitScorer.score(targetStructure, path)))
                // hits filtered by high RMSD value are reported as null
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
                .get();
    }
}
