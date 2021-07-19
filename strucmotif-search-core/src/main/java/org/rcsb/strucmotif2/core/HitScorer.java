package org.rcsb.strucmotif2.core;

import org.rcsb.strucmotif2.domain.AtomPairingScheme;
import org.rcsb.strucmotif2.domain.result.Hit;
import org.rcsb.strucmotif2.domain.result.SimpleHit;
import org.rcsb.strucmotif2.domain.result.TransformedHit;

import java.util.List;
import java.util.stream.Stream;

/**
 * Quantifies how well a {@link SimpleHit} resembles the query motif.
 */
public interface HitScorer {
    /**
     * Scores this collection of residues (a.k.a. a path through the structure).
     * @param simpleHit the original hit that should be scored
     * @return a {@link Hit} instance
     */
    Stream<TransformedHit> score(List<SimpleHit> simpleHit);

    /**
     * Reports the strategy used to pair atoms.
     * @return an {@link AtomPairingScheme}
     */
    AtomPairingScheme getAtomPairingScheme();
}
