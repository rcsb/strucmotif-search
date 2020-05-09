package org.rcsb.strucmotif.domain.selection;

import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to resolve {@link AuthorSelection} instances.
 */
public class AuthorSelectionResolver implements SelectionResolver<AuthorSelection> {
    private final Map<Residue, AuthorSelection> forward;
    private final Map<AuthorSelection, Residue> backward;

    public AuthorSelectionResolver(Structure structure) {
        this.forward = new HashMap<>();
        this.backward = new HashMap<>();
        for (Chain chain : structure.getChains()) {
            ChainIdentifier chainIdentifier = chain.getChainIdentifier();
            String authAsymId = chainIdentifier.getAuthAsymId();
            int assemblyId = chainIdentifier.getAssemblyId();
            for (Residue residue : chain.getResidues()) {
                AuthorSelection selector = new AuthorSelection(authAsymId, assemblyId, residue.getResidueIdentifier().getAuthSeqId());
                forward.put(residue, selector);
                backward.put(selector, residue);
            }
        }
    }

    @Override
    public Residue resolve(AuthorSelection selector) {
        return backward.get(selector);
    }

    @Override
    public AuthorSelection resolve(Residue residue) {
        return forward.get(residue);
    }
}
