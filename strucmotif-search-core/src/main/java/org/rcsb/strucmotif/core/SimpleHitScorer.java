package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

public class SimpleHitScorer implements HitScorer {
    @Override
    public Hit score(TargetStructure targetStructure, Pair<List<Residue>, Integer> targetResidues) {
        return null;
    }

    @Override
    public double getScoreCutoff() {
        return 0;
    }
}
