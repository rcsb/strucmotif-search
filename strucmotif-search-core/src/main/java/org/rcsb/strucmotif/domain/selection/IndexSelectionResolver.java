package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to bidirectionally map between {@link IndexSelection} and the corresponding {@link Residue}.
 */
public class IndexSelectionResolver implements SelectionResolver<IndexSelection> {
    private final Map<Residue, IndexSelection> forward;
    private final Map<IndexSelection, Residue> backward;

    public IndexSelectionResolver(Structure structure) {
        this.forward = new HashMap<>();
        this.backward = new HashMap<>();
        for (Chain chain : structure.getChains()) {
            String structOperId = chain.getChainIdentifier().getStructOperId();
            for (Residue residue : chain.getResidues()) {
                IndexSelection selector = new IndexSelection(structOperId, residue.getResidueIdentifier().getIndex());
                forward.put(residue, selector);
                backward.put(selector, residue);
            }
        }
    }

    @Override
    public Residue resolve(IndexSelection selection) {
        return backward.get(selection);
    }

    @Override
    public IndexSelection resolve(Residue residue) {
        return forward.get(residue);
    }
}
