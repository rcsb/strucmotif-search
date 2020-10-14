package org.rcsb.strucmotif;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.core.HitScorer;
import org.rcsb.strucmotif.core.RootMeanSquareDeviationHitScorer;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;

/**
 * The entry point to perform motif searches.
 */
public class MotifSearch {
    private final QueryBuilder queryBuilder;
    private final AlignmentService alignmentService;
    private final StructureDataProvider structureDataProvider;
    private static final MotifSearch INSTANCE = new MotifSearch();

    private MotifSearch() {
        MotifSearchApplication.main(new String[0]);
        this.queryBuilder = MotifSearchApplication.queryBuilder;
        this.alignmentService = MotifSearchApplication.alignmentService;
        this.structureDataProvider = MotifSearchApplication.structureDataProvider;
    }

    /**
     * Start a new query using the {@link QueryBuilder}.
     * @return a fresh builder instance
     */
    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }

    /**
     * Creates a {@link HitScorer} that derives better scores for potential hits.
     * @param queryStructure the reference structure
     * @param atomPairingScheme how to pair atoms?
     * @return a {@link HitScorer} instance ready to process hits in the context of the given query structure
     */
    public static HitScorer newHitScorer(Structure queryStructure, AtomPairingScheme atomPairingScheme) {
        return new RootMeanSquareDeviationHitScorer(queryStructure, atomPairingScheme, INSTANCE.alignmentService, INSTANCE.structureDataProvider);
    }
}
