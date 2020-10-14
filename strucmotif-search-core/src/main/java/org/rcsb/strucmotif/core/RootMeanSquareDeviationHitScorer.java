package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.result.SimpleHit;
import org.rcsb.strucmotif.domain.result.TransformedHit;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class RootMeanSquareDeviationHitScorer implements HitScorer {
    private final List<Residue> queryResidues;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignmentService;
    private final StructureDataProvider structureDataProvider;

    public RootMeanSquareDeviationHitScorer(Structure queryStructure, AtomPairingScheme atomPairingScheme, AlignmentService alignmentService, StructureDataProvider structureDataProvider) {
        this.queryResidues = queryStructure.getChains()
                .stream()
                .map(Chain::getResidues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        this.atomPairingScheme = atomPairingScheme;
        this.alignmentService = alignmentService;
        this.structureDataProvider = structureDataProvider;
    }

    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    @Override
    public TransformedHit score(SimpleHit simpleHit) {
        Structure targetStructure = structureDataProvider.readRenumbered(simpleHit.getStructureIdentifier(), simpleHit.getSelection());
        List<Residue> targetResidues = targetStructure.getChains()
                .stream()
                .map(Chain::getResidues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        AlignmentResult alignmentResult = alignmentService.align(queryResidues, targetResidues, atomPairingScheme);

        return new TransformedHit(simpleHit,
                targetResidues.stream().map(Residue::getResidueIdentifier).map(ResidueIdentifier::getResidueType).collect(Collectors.toList()),
                alignmentResult.getRootMeanSquareDeviation(),
                alignmentResult.getTransformation());
    }
}
