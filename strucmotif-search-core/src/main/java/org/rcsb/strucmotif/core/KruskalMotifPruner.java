package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.ResidueGraph;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
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
    private final MotifSearchConfig motifSearchConfig;

    /**
     * Injectable constructor.
     * @param motifSearchConfig app config
     */
    @Autowired
    public KruskalMotifPruner(MotifSearchConfig motifSearchConfig) {
        this.motifSearchConfig = motifSearchConfig;
    }

    @Override
    public List<ResiduePairOccurrence> prune(Structure structure) {
        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig.getSquaredDistanceCutoff(), true);

        List<ResiduePairOccurrence> residuePairOccurrences = residueGraph.residuePairOccurrencesSequential()
                .collect(Collectors.toList());

        long residueCount = structure.getChains()
                .stream()
                .map(Chain::getResidues)
                .mapToLong(Collection::size)
                .sum();

        // ignore motifs with <4 identifiers
        if (residueCount < 4) {
            return residuePairOccurrences;
        }

        return kruskal(residuePairOccurrences);
    }

    private List<ResiduePairOccurrence> kruskal(List<ResiduePairOccurrence> residuePairOccurrences) {
        List<Set<LabelSelection>> coveredSelectors = new ArrayList<>();
        List<ResiduePairOccurrence> result = new ArrayList<>();
        // sort all edges by weight
        residuePairOccurrences.sort(Comparator.comparingInt(mo -> mo.getResiduePairDescriptor().getBackboneDistance().ordinal()));

        while (residuePairOccurrences.size() > 0) {
            ResiduePairOccurrence best = residuePairOccurrences.remove(0);
            LabelSelection id1 = best.getResidueIdentifier().getLabelSelection1();
            LabelSelection id2 = best.getResidueIdentifier().getLabelSelection2();

            // prevent formation of circles
            Set<LabelSelection> set1 = find(coveredSelectors, id1);
            Set<LabelSelection> set2 = find(coveredSelectors, id2);
            if (set1 == null || !set1.equals(set2)) {
                result.add(best);

                Set<LabelSelection> updated = new HashSet<>();
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

    private Set<LabelSelection> find(List<Set<LabelSelection>> selectors, LabelSelection selector) {
        return selectors.stream()
                .filter(list -> list.contains(selector))
                .findFirst()
                .orElse(null);
    }
}
