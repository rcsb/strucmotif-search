package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.TargetStructure;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.selection.SelectionResolver;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.List;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class HitScorerImpl implements HitScorer {
    private final List<Residue> queryResidues;
    private final double rmsdCutoff;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignment;

    public HitScorerImpl(QueryStructure queryStructure, double rmsdCutoff, AtomPairingScheme atomPairingScheme, AlignmentService alignment) {
        this.queryResidues = queryStructure.getResidues();
        this.rmsdCutoff = rmsdCutoff;
        this.atomPairingScheme = atomPairingScheme;
        this.alignment = alignment;
    }

    @Override
    public double getRmsdCutoff() {
        return rmsdCutoff;
    }

    @Override
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    @Override
    public Hit score(TargetStructure targetStructure, List<Residue> targetResidues) {
        AlignmentResult alignmentResult = alignment.align(queryResidues, targetResidues, atomPairingScheme);

        // filtered hits are reported as null, this feels hacky but should save some time
        if (rmsdCutoff < alignmentResult.getScore().doubleValue()) {
            return null;
        }

        // resolve identifiers
        SelectionResolver<LabelSelection> labelSelectionResolver = targetStructure.getLabelSelectionResolver();
        List<Residue> originalCandidate = alignmentResult.getOriginalCandidate();

        LabelSelection[] selection = new LabelSelection[originalCandidate.size()];
        ResidueType[] residueTypes = new ResidueType[originalCandidate.size()];
        for (int i = 0; i < originalCandidate.size(); i++) {
            Residue originalResidue = originalCandidate.get(i);
            selection[i] = labelSelectionResolver.resolve(originalResidue);
            residueTypes[i] = originalResidue.getResidueIdentifier().getResidueType();
        }

        // flatten transformation into 1d array
        double[] transformation = new double[16];
        double[][] transMat = alignmentResult.getTransformation().getTransformation();
        int n = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                transformation[n++] = transMat[i][j];
            }
        }

        return new Hit(targetStructure.getStructureIdentifier(),
                selection,
                residueTypes,
                alignmentResult.getScore().doubleValue(),
                transformation);
    }
}
