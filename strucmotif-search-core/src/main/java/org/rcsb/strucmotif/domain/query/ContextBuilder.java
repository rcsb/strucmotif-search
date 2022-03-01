package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.domain.SearchContext;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;

public interface ContextBuilder<B extends ContextBuilder.MandatoryBuilder<?, C>, C extends SearchContext<?, ?, ?, ?, ?>> {
    interface MandatoryBuilder<B, C> {
        B backboneDistanceTolerance(int backboneDistanceTolerance);

        B sideChainDistanceTolerance(int sideChainDistanceTolerance);

        B angleTolerance(int angleTolerance);

        B rmsdCutoff(double rmsdCutoff);

        B atomPairingScheme(AtomPairingScheme atomPairingScheme);

        B motifPruningStrategy(MotifPruner motifPruner);

        B motifPruningStrategy(MotifPruningStrategy motifPruningStrategy);

        OptionalBuilder<C> buildParameters();
    }

    interface OptionalBuilder<C> {
        C buildContext();
    }
}
