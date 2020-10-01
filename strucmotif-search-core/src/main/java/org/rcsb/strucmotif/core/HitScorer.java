package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

/**
 * Quantifies how well a {@link TargetStructure} resembles the query motif.
 */
public interface HitScorer {
    /**
     * Scores this collection of residues (a.k.a. a path through the structure).
     * @param targetStructure the parent container - needed to infer some entry-level information such as id
     * @param targetResidues the residues to align
     * @return a {@link Hit} instance - <code>null</code> if filtered for high RMSD
     */
    Hit score(TargetStructure targetStructure, Pair<List<Residue>, Integer> targetResidues);

    /**
     * The value above which the implementation will return <code>null</code> rather than a result instance. By
     * contract, this will be used to filter away hits that aren't desired in the result set.
     * The value above which null will be returned rather than a result instance. By contract, this will be used to
     * filter away hits that aren't desired in the result set.
     * @return the RMSD threshold for filtering
     */
    double getRmsdCutoff();

    /**
     * How is atom correspondence determined?
     * @return an {@link AtomPairingScheme}
     */
    AtomPairingScheme getAtomPairingScheme();
}
