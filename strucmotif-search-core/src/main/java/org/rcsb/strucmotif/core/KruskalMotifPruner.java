package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A motif may be best described by the minimal spanning tree which connects all vertices in the most compact fashion.
 * Uses Kruskal's algorithm to determine tree.
 */
@Service
public class KruskalMotifPruner implements MotifPruner {
    /**
     * Default constructor.
     */
    public KruskalMotifPruner() {
    }

    @Override
    public List<ResiduePairOccurrence> prune(ResidueGraph residueGraph) {
        List<ResiduePairOccurrence> residuePairOccurrences = residueGraph.residuePairOccurrencesSequential()
                .sorted(ResiduePairOccurrence.INFORMATIVENESS_COMPARATOR)
                // can't be toList() as shuffle will happen downstream
                .collect(Collectors.toList());

        // ignore motifs with <4 identifiers
        if (residueGraph.getResidueCount() < 4) {
            return residuePairOccurrences;
        }

        return kruskal(residuePairOccurrences);
    }

    private List<ResiduePairOccurrence> kruskal(List<ResiduePairOccurrence> residuePairOccurrences) {
        List<Set<Integer>> coveredSelectors = new ArrayList<>();
        List<ResiduePairOccurrence> result = new ArrayList<>();

        while (!residuePairOccurrences.isEmpty()) {
            ResiduePairOccurrence best = residuePairOccurrences.remove(0);
            int id1 = best.getResidueIndex1();
            int id2 = best.getResidueIndex2();

            // prevent formation of circles
            Set<Integer> set1 = find(coveredSelectors, id1);
            Set<Integer> set2 = find(coveredSelectors, id2);
            if (set1 == null || !set1.equals(set2)) {
                result.add(best);

                Set<Integer> updated = new LinkedHashSet<>();
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

    private Set<Integer> find(List<Set<Integer>> selectors, Integer selector) {
        return selectors.stream()
                .filter(list -> list.contains(selector))
                .findFirst()
                .orElse(null);
    }
}
