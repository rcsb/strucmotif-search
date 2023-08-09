package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.SearchContext;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

/**
 * Step-wise builder of the search context.
 */
public interface ContextBuilder {
    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     * @param <C> type of the context to create
     */
    interface MandatoryBuilder<C extends SearchContext<?, ?, ?>> {
        /**
         * Specify the backbone distance tolerance (default: 1).
         * @param backboneDistanceTolerance an int
         * @return this builder
         */
        MandatoryBuilder<?> backboneDistanceTolerance(int backboneDistanceTolerance);

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance an int
         * @return this builder
         */
        MandatoryBuilder<?> sideChainDistanceTolerance(int sideChainDistanceTolerance);

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance an int
         * @return this builder
         */
        MandatoryBuilder<?> angleTolerance(int angleTolerance);

        /**
         * Filter hits based on RMSD.
         * @param rmsdCutoff the RMSD cutoff above which hits are filtered
         * @return this builder
         */
        MandatoryBuilder<?> rmsdCutoff(double rmsdCutoff);

        /**
         * Controls which atoms will be considered for alignment. Only relevant when scoring scheme is alignment-based.
         * @param atomPairingScheme how to pair atoms for alignment routine
         * @return this builder
         */
        MandatoryBuilder<?> atomPairingScheme(AtomPairingScheme atomPairingScheme);

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        MandatoryBuilder<?> motifPruningStrategy(MotifPruner motifPruner);

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        MandatoryBuilder<?> motifPruningStrategy(MotifPruningStrategy motifPruningStrategy);

        /**
         * Timeout after this many milliseconds by throwing a {@link java.util.concurrent.TimeoutException}. Set to
         * <code>Integer.MAX_VALUE</code> to not enforce any timeout.
         * @param ms the timeout in ms
         * @return this builder
         */
        MandatoryBuilder<?> timeout(int ms);

        /**
         * Create the parameter object and move on to the optional step.
         * @return the optional builder instance
         */
        OptionalBuilder<C> buildParameters();
    }

    /**
     * Optional values.
     * @param <C> type of the context to create
     */
    interface OptionalBuilder<C extends SearchContext<?, ?, ?>> {
        /**
         * Create the context.
         * @return a context
         */
        C buildContext();
    }
}
