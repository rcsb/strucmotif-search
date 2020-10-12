package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.SimpleHit;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.result.TransformedHit;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.selection.SelectionResolver;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class RootMeanSquareDeviationHitScorer implements HitScorer {
    private final List<Residue> queryResidues;
    private final double rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignment;

    public RootMeanSquareDeviationHitScorer(QueryStructure queryStructure, double rmsdCutoff, AtomPairingScheme atomPairingScheme, AlignmentService alignment) {
        this.queryResidues = queryStructure.getResidues();
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.alignment = alignment;
    }

    @Override
    public double getScoreCutoff() {
        return rmsdCutoff;
    }

    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    @Override
    public TransformedHit score(TargetStructure targetStructure, Pair<List<Residue>, Integer> targetResidues) {
        AlignmentResult alignmentResult = alignment.align(queryResidues, targetResidues.getFirst(), atomPairingScheme);

        // filtered hits are reported as null, this feels hacky but should save some time
        if (rmsdCutoff < alignmentResult.getRootMeanSquareDeviation().value()) {
            return null;
        }

        // resolve identifiers
        SelectionResolver<LabelSelection> labelSelectionResolver = targetStructure.getLabelSelectionResolver();
        List<Residue> originalCandidate = alignmentResult.getOriginalCandidate();

        List<LabelSelection> selection = new ArrayList<>();
        List<ResidueType> residueTypes = new ArrayList<>();
        for (Residue originalResidue : originalCandidate) {
            selection.add(labelSelectionResolver.resolve(originalResidue));
            residueTypes.add(originalResidue.getResidueIdentifier().getResidueType());
        }

        SimpleHit simpleHit = new SimpleHit(targetStructure.getStructureIdentifier(),
                selection,
                null); // TODO impl
        return new TransformedHit(simpleHit,
                residueTypes,
                alignmentResult.getRootMeanSquareDeviation(),
                alignmentResult.getTransformation());
    }
}
