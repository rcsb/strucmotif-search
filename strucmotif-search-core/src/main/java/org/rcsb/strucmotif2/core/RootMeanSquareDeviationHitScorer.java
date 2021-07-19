package org.rcsb.strucmotif2.core;

import org.rcsb.strucmotif2.align.AlignmentService;
import org.rcsb.strucmotif2.domain.AlignmentResult;
import org.rcsb.strucmotif2.domain.AtomPairingScheme;
import org.rcsb.strucmotif2.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif2.domain.result.SimpleHit;
import org.rcsb.strucmotif2.domain.result.TransformedHit;
import org.rcsb.strucmotif2.domain.selection.LabelSelection;
import org.rcsb.strucmotif2.domain.selection.LabelSelectionResolver;
import org.rcsb.strucmotif2.domain.structure.Chain;
import org.rcsb.strucmotif2.domain.structure.Residue;
import org.rcsb.strucmotif2.domain.structure.Structure;
import org.rcsb.strucmotif2.io.StructureDataProvider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scores hits by computing the RMSD with respect to the reference motif.
 */
public class RootMeanSquareDeviationHitScorer implements HitScorer {
    private final List<Residue> queryResidues;
    private final AtomPairingScheme atomPairingScheme;
    private final AlignmentService alignmentService;
    private final StructureDataProvider structureDataProvider;

    /**
     * Construct a query-specific hit scorer.
     * @param queryStructure the reference structure
     * @param atomPairingScheme how to pair atoms?
     * @param alignmentService alignment service
     * @param structureDataProvider structure data provider
     */
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

    @Override
    public AtomPairingScheme getAtomPairingScheme() {
        return atomPairingScheme;
    }

    @Override
    public Stream<TransformedHit> score(List<SimpleHit> simpleHits) {
        List<LabelSelection> allResidues = simpleHits.stream()
                .map(SimpleHit::getSelection)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        Structure parentStructure = structureDataProvider.readRenumbered(simpleHits.get(0).getStructureIdentifier(), allResidues);
        LabelSelectionResolver labelSelectionResolver = new LabelSelectionResolver(parentStructure);

        return simpleHits.stream()
                .map(h -> {
                    List<Residue> targetResidues = h.getSelection()
                            .stream()
                            .map(labelSelectionResolver::resolve)
                            .collect(Collectors.toList());
                    AlignmentResult alignmentResult = alignmentService.align(queryResidues, targetResidues, atomPairingScheme);

                    return new TransformedHit(h,
                            targetResidues.stream().map(Residue::getResidueIdentifier).map(ResidueIdentifier::getResidueType).collect(Collectors.toList()),
                            alignmentResult.getRootMeanSquareDeviation(),
                            alignmentResult.getTransformation());
                });
    }
}
