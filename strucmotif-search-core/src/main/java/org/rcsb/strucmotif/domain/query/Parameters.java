package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.AtomPairingScheme;

import java.util.concurrent.ForkJoinPool;

/**
 * Immutable instance capturing all parameters of a given search job.
 */
public class Parameters {
    private final int backboneDistanceTolerance;
    private final int sideChainDistanceTolerance;
    private final int angleTolerance;
    private final boolean parallel;
    private final int numberThreads;
    private final double rmsdCutoff;
    private final MotifPruner motifPruner;
    private final AtomPairingScheme atomPairingScheme;
    private final int limit;
    private final ForkJoinPool forkJoinPool;
    static final int DEFAULT_BACKBONE_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE = 1;
    static final int DEFAULT_ANGLE_TOLERANCE = 1;
    static final boolean DEFAULT_PARALLELISM = true;
    static final double DEFAULT_RMSD_CUTOFF = 2.0;

    Parameters(int backboneDistanceTolerance, int sideChainDistanceTolerance, int angleTolerance, boolean parallel, double rmsdCutoff, MotifPruner motifPruner, AtomPairingScheme atomPairingScheme, int resultLimit) {
        this.backboneDistanceTolerance = backboneDistanceTolerance;
        this.sideChainDistanceTolerance = sideChainDistanceTolerance;
        this.angleTolerance = angleTolerance;
        this.parallel = parallel;
        this.numberThreads = parallel ? Runtime.getRuntime().availableProcessors() : 1;
        this.rmsdCutoff = rmsdCutoff == 0 ? Double.MAX_VALUE : rmsdCutoff;
        this.motifPruner = motifPruner;
        this.atomPairingScheme = atomPairingScheme;
        this.limit = resultLimit == 0 ? Integer.MAX_VALUE : resultLimit;
        this.forkJoinPool = new ForkJoinPool(numberThreads);
    }

    /**
     * The specified backbone distance tolerance value.
     * @return the tolerance value
     */
    public int getBackboneDistanceTolerance() {
        return backboneDistanceTolerance;
    }

    /**
     * The specified side-chain distance tolerance value.
     * @return the tolerance value
     */
    public int getSideChainDistanceTolerance() {
        return sideChainDistanceTolerance;
    }

    /**
     * The specified angle tolerance value.
     * @return the tolerance value
     */
    public int getAngleTolerance() {
        return angleTolerance;
    }

    /**
     * Currently rather meaningless. Parallelization will utilize common thread pool - number of threads = maximum.
     * @return the number of threads to be used
     */
    public int getNumberThreads() {
        return numberThreads;
    }

    /**
     * At which RMSD are hits filtered?
     * @return the cutoff value in Angstrom
     */
    public double getRmsdCutoff() {
        return rmsdCutoff;
    }

    /**
     * The motif pruning strategy applied.
     * @return motif pruning impl
     */
    public MotifPruner getMotifPruner() {
        return motifPruner;
    }

    /**
     * The atom pairing strategy applied.
     * @return atom pairing scheme
     */
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    public int getLimit() {
        return limit;
    }

    public boolean hasLimit() {
        return limit != Integer.MAX_VALUE;
    }

    public ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }

    /**
     * Execute in parallel?
     * @return true if collections are supposed to be handled by parallel streams
     */
    public boolean isParallel() {
        return parallel;
    }
}
