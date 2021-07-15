package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SimpleHit;
import org.rcsb.strucmotif.domain.result.TransformedHit;
import org.rcsb.strucmotif.domain.structure.Structure;

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
     * The reference structure with respect to which hits will be aligned.
     * @return the reference structure of this hit scorer
     */
    Structure getQueryStructure();

    /**
     * Reports the strategy used to pair atoms.
     * @return an {@link AtomPairingScheme}
     */
    AtomPairingScheme getAtomPairingScheme();
}
