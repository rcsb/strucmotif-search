package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A motif may be best described by the minimal spanning tree which connects all vertices in the most compact fashion.
 * Uses Kruskal's algorithm to determine tree.
 */
@Service
public class KruskalMotifPruner implements MotifPruner {
    @Override
    public List<ResiduePairOccurrence> prune(ResidueGraph residueGraph) {
        List<ResiduePairOccurrence> residuePairOccurrences = residueGraph.residuePairOccurrencesSequential()
                .collect(Collectors.toList());

        // ignore motifs with <4 identifiers
        if (residueGraph.getNumberOfResidues() < 4) {
            return residuePairOccurrences;
        }

        return kruskal(residuePairOccurrences);
    }

    private List<ResiduePairOccurrence> kruskal(List<ResiduePairOccurrence> residuePairOccurrences) {
        List<Set<IndexSelection>> coveredSelectors = new ArrayList<>();
        List<ResiduePairOccurrence> result = new ArrayList<>();
        // sort all edges by weight
        residuePairOccurrences.sort(Comparator.comparingInt(mo -> mo.getResiduePairDescriptor().getBackboneDistance().ordinal()));

        while (!residuePairOccurrences.isEmpty()) {
            ResiduePairOccurrence best = residuePairOccurrences.remove(0);
            IndexSelection id1 = best.getResidueIdentifier().getIndexSelection1();
            IndexSelection id2 = best.getResidueIdentifier().getIndexSelection2();

            // prevent formation of circles
            Set<IndexSelection> set1 = find(coveredSelectors, id1);
            Set<IndexSelection> set2 = find(coveredSelectors, id2);
            if (set1 == null || !set1.equals(set2)) {
                result.add(best);

                Set<IndexSelection> updated = new HashSet<>();
                if (set1 != null) {
                    coveredSelectors.remove(set1);
                    updated.addAll(set1);
                }
                if (set2 != null) {
                    coveredSelectors.remove(set2);
                    updated.addAll(set2);
                }
                updated.add(id1);
                updated.add(id2);
                coveredSelectors.add(updated);
            }
        }

        return result;
    }

    private Set<IndexSelection> find(List<Set<IndexSelection>> selectors, IndexSelection selector) {
        return selectors.stream()
                .filter(list -> list.contains(selector))
                .findFirst()
                .orElse(null);
    }
}
