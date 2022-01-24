package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A query structure wraps a {@link Structure} and provides additional functionality needed to employ it as motif
 * definition during a structural motif query job.
 */
public class QueryStructure {
    private final String structureIdentifier;
    private final Structure structure;
    private final List<IndexSelection> indexSelections;
    private final List<Map<LabelAtomId, float[]>> residues;
    private final List<ResiduePairOccurrence> residuePairOccurrences;
    private final List<ResiduePairIdentifier> residuePairIdentifiers;
    private final List<ResiduePairDescriptor> residuePairDescriptors;
    private final List<Integer> residueIndexSwaps;

    QueryStructure(String structureIdentifier, Structure structure, List<LabelSelection> originalLabelSelections, List<Map<LabelAtomId, float[]>> originalResidues, List<ResiduePairOccurrence> residuePairOccurrences, Map<LabelSelection, Set<ResidueType>> exchanges) {
        this.structureIdentifier = structureIdentifier;
        this.structure = structure;
        if (residuePairOccurrences.isEmpty()) {
            throw new IllegalQueryDefinitionException("Did not find any residue pairs in structure - check query definition");
        }

        // sort occurrences to ensure that no dangling words are encountered during path assembly
        // this prevents spikes in runtime where no checks can be performed and the number of paths to evaluate subsequently explodes
        List<ResiduePairOccurrence> connectedResiduePairs = getPathOfConnectedResiduePairs(residuePairOccurrences, exchanges);

        this.residuePairOccurrences = connectedResiduePairs;
        this.residuePairIdentifiers = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .collect(Collectors.toList());
        this.residuePairDescriptors = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .collect(Collectors.toList());

        // explode query into motifs and get entities by that - this provides the correct order of entities so that the
        // alignment routine does not have to care about finding correspondence
        this.indexSelections = residuePairIdentifiers.stream()
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .collect(Collectors.toList());

        if (indexSelections.size() != originalResidues.size()) {
            // this indicates that fewer residues are present in the result than specified by the query
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }

        List<IndexSelection> originalIndexSelections = originalLabelSelections.stream()
                .map(labelSelection -> {
                    int residueIndex = structure.getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId());
                    return new IndexSelection(labelSelection.getStructOperId(), residueIndex);
                })
                .collect(Collectors.toList());
        this.residueIndexSwaps = originalIndexSelections.stream()
                .map(indexSelections::indexOf)
                .collect(Collectors.toList());
        this.residues = originalResidues;
    }

    /**
     * Determine a unique path through this structure which captures/passes all residue pairs. Each residue must be
     * present at least once.
     * @param residuePairOccurrences the collection of residue pair occurrences to process
     * @param exchanges set of exchanges (exchange-heavy residues will get evaluated late/last)
     * @return a filtered collection of residue pair occurrences
     */
    private List<ResiduePairOccurrence> getPathOfConnectedResiduePairs(List<ResiduePairOccurrence> residuePairOccurrences, Map<LabelSelection, Set<ResidueType>> exchanges) {
        Map<IndexSelection, Integer> exchangeCounts = residuePairOccurrences.stream()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), i -> exchangeCount(i, exchanges)));
        Map<IndexSelection, Long> connectionCounts = residuePairOccurrences.stream()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<ResiduePairOccurrence> sparse = new ArrayList<>();
        List<ResiduePairOccurrence> sorted = residuePairOccurrences.stream()
                // have pairs with many exchanges last (as they are expensive to evaluate)
                .sorted(Comparator.comparingInt((ResiduePairOccurrence o) -> exchangeCounts.get(o.getResidueIdentifier().getIndexSelection1()) + exchangeCounts.get(o.getResidueIdentifier().getIndexSelection2()))
                        // move pairs with many connections to the front
                        .thenComparing((ResiduePairOccurrence o) -> connectionCounts.get(o.getResidueIdentifier().getIndexSelection1()) + connectionCounts.get(o.getResidueIdentifier().getIndexSelection2())))
                .collect(Collectors.toList());
        // assign 'random' word as start
        sparse.add(sorted.remove(0));

        while (sorted.size() > 0) {
            for (int i = 0; i < sorted.size(); i++) {
                ResiduePairOccurrence candidateResiduePair = sorted.get(i);
                ResiduePairIdentifier candidateIdentifier = candidateResiduePair.getResidueIdentifier();
                if (sparse.stream()
                        // check for overlap with already accepted word
                        .anyMatch(sortedResiduePair -> match(sortedResiduePair.getResidueIdentifier(), candidateIdentifier))) {
                    sparse.add(sorted.remove(i));
                    break;
                }
            }
        }

        return sparse;
    }

    private int exchangeCount(IndexSelection indexSelection, Map<LabelSelection, Set<ResidueType>> exchanges) {
        LabelSelection l = structure.getLabelSelection(indexSelection.getIndex());
        LabelSelection labelSelection = new LabelSelection(l.getLabelAsymId(), indexSelection.getStructOperId(), l.getLabelSeqId());

        return (exchanges.containsKey(labelSelection) ? exchanges.get(labelSelection).size() : 0);
    }

    /**
     * Check if two identifiers reference an overlapping index selection.
     * @param sortedWordResiduePairIdentifier reference
     * @param candidateIdentifier candidate
     * @return true if describing an overlapping selection
     */
    private boolean match(ResiduePairIdentifier sortedWordResiduePairIdentifier, ResiduePairIdentifier candidateIdentifier) {
        return sortedWordResiduePairIdentifier.getIndexSelection1().equals(candidateIdentifier.getIndexSelection1()) ||
                sortedWordResiduePairIdentifier.getIndexSelection1().equals(candidateIdentifier.getIndexSelection2()) ||
                sortedWordResiduePairIdentifier.getIndexSelection2().equals(candidateIdentifier.getIndexSelection1()) ||
                sortedWordResiduePairIdentifier.getIndexSelection2().equals(candidateIdentifier.getIndexSelection2());
    }

    /**
     * Get the identifier of this query structure.
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Delegate to wrapped structure.
     * @return the structure instance
     */
    public Structure getStructure() {
        return structure;
    }

    /**
     * All word occurrences in this query structure.
     * @return a collection of word occurrences
     */
    public List<ResiduePairOccurrence> getResiduePairOccurrences() {
        return residuePairOccurrences;
    }

    /**
     * All word identifiers in this query structure.
     * @return a collection of word identifiers
     */
    public List<ResiduePairIdentifier> getResiduePairIdentifiers() {
        return residuePairIdentifiers;
    }

    /**
     * All word descriptors in this query structure.
     * @return a collection of word descriptors
     */
    public List<ResiduePairDescriptor> getResiduePairDescriptors() {
        return residuePairDescriptors;
    }

    /**
     * Get all residues of this query structure.
     * @return a collection of residues
     */
    public List<Map<LabelAtomId, float[]>> getResidues() {
        return residues;
    }

    /**
     * All selections of this query structure.
     * @return a collection of IndexSelections
     */
    public List<IndexSelection> getIndexSelections() {
        return indexSelections;
    }

    /**
     * This allows re-arranging residues in hits, so they have the same order as the query.
     * @return an array tracks how residues were swapped
     */
    public List<Integer> getResidueIndexSwaps() {
        return residueIndexSwaps;
    }
}
