package org.rcsb.strucmotif.core;

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
     * @param targetStructure the parent container - needed to infer some entry-level information such as id and title
     * @param targetResidues the residues to align
     * @return a {@link Hit} instance - <code>null</code> if filtered for high RMSD
     */
    Hit score(TargetStructure targetStructure, List<Residue> targetResidues);
}
