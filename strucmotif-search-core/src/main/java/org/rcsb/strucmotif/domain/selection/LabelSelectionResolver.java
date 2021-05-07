package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to bidirectionally map between {@link LabelSelection} and the corresponding {@link Residue}.
 */
public class LabelSelectionResolver implements SelectionResolver<LabelSelection> {
    private final Map<Residue, LabelSelection> forward;
    private final Map<LabelSelection, Residue> backward;

    /**
     * Initialize a selection resolver on a structure.
     * @param structure the structure to process
     */
    public LabelSelectionResolver(Structure structure) {
        this.forward = new HashMap<>();
        this.backward = new HashMap<>();
        for (Chain chain : structure.getChains()) {
            ChainIdentifier chainIdentifier = chain.getChainIdentifier();
            String labelAsymId = chainIdentifier.getLabelAsymId();
            String structOperId = chainIdentifier.getStructOperId();
            for (Residue residue : chain.getResidues()) {
                LabelSelection selector = new LabelSelection(labelAsymId, structOperId, residue.getResidueIdentifier().getLabelSeqId());
                forward.put(residue, selector);
                backward.put(selector, residue);
            }
        }
    }

    @Override
    public Residue resolve(LabelSelection selection) {
        return backward.get(selection);
    }

    @Override
    public LabelSelection resolve(Residue residue) {
        return forward.get(residue);
    }
}
