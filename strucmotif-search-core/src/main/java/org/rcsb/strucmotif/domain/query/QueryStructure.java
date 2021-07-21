package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A query structure wraps a {@link Structure} and provides additional functionality needed to employ it as motif
 * definition during a structural motif query job.
 */
public class QueryStructure {
    private final String structureIdentifier;
    private final Structure structure;
    private final List<LabelSelection> labelSelections;
    private final List<Map<String, float[]>> residues;
    private final List<ResiduePairOccurrence> residuePairOccurrences;
    private final List<ResiduePairIdentifier> residuePairIdentifiers;
    private final List<ResiduePairDescriptor> residuePairDescriptors;
    private final List<Integer> residueIndexSwaps;

    QueryStructure(String structureIdentifier, Structure structure, List<LabelSelection> originalLabelSelections, List<Map<String, float[]>> originalResidues, List<ResiduePairOccurrence> residuePairOccurrences) {
        this.structureIdentifier = structureIdentifier;
        this.structure = structure;
        if (residuePairOccurrences.isEmpty()) {
            throw new IllegalQueryDefinitionException("Did not find any residue pairs in structure - check query definition");
        }

        // sort occurrences to ensure that no dangling words are encountered during path assembly
        // this prevents spikes in runtime where no checks can be performed and the number of paths to evaluate subsequently explodes
        List<ResiduePairOccurrence> connectedResiduePairs = getPathOfConnectedResiduePairs(residuePairOccurrences);

        this.residuePairOccurrences = connectedResiduePairs;
        this.residuePairIdentifiers = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .collect(Collectors.toList());
        this.residuePairDescriptors = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .collect(Collectors.toList());

        // explode query into motifs and get entities by that - this provides the correct order of entities so that the
        // alignment routine does not have to care about finding correspondence
        this.labelSelections = residuePairIdentifiers.stream()
                .flatMap(ResiduePairIdentifier::labelSelections)
                .distinct()
                .collect(Collectors.toList());

        if (labelSelections.size() != originalResidues.size()) {
            // this indicates that fewer residues are present in the result than specified by the query
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }

        this.residueIndexSwaps = originalLabelSelections.stream()
                .map(labelSelections::indexOf)
                .collect(Collectors.toList());
        this.residues = residueIndexSwaps.stream()
                .map(originalResidues::get)
                .collect(Collectors.toList());
    }

    /**
     * Determine an unique path through this structure which captures/passes all residue pairs. Each residue must be
     * present at least once.
     * @param residuePairOccurrences the collection of residue pair occurrences to process
     * @return a filtered collection of residue pair occurrences
     */
    private List<ResiduePairOccurrence> getPathOfConnectedResiduePairs(List<ResiduePairOccurrence> residuePairOccurrences) {
        // TODO prolly is beneficial to move most connected words to the front
        List<ResiduePairOccurrence> sorted = new ArrayList<>();
        List<ResiduePairOccurrence> toConsume = new ArrayList<>(residuePairOccurrences);
        // assign 'random' word as start
        sorted.add(toConsume.remove(0));

        while (toConsume.size() > 0) {
            for (int i = 0; i < toConsume.size(); i++) {
                ResiduePairOccurrence candidateResiduePair = toConsume.get(i);
                ResiduePairIdentifier candidateIdentifier = candidateResiduePair.getResidueIdentifier();
                if (sorted.stream()
                        .anyMatch(sortedResiduePair -> match(sortedResiduePair.getResidueIdentifier(), candidateIdentifier))) {
                    sorted.add(toConsume.remove(i));
                    break;
                }
            }
        }

        return sorted;
    }

    /**
     * Check if two identifiers reference an overlapping index selection.
     * @param sortedWordResiduePairIdentifier reference
     * @param candidateIdentifier candidate
     * @return true if describing an overlapping selection
     */
    private boolean match(ResiduePairIdentifier sortedWordResiduePairIdentifier, ResiduePairIdentifier candidateIdentifier) {
        return sortedWordResiduePairIdentifier.getLabelSelection1().equals(candidateIdentifier.getLabelSelection1()) ||
                sortedWordResiduePairIdentifier.getLabelSelection1().equals(candidateIdentifier.getLabelSelection2()) ||
                sortedWordResiduePairIdentifier.getLabelSelection2().equals(candidateIdentifier.getLabelSelection1()) ||
                sortedWordResiduePairIdentifier.getLabelSelection2().equals(candidateIdentifier.getLabelSelection2());
    }

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

    public List<Map<String, float[]>> getResidues() {
        return residues;
    }

    /**
     * All selections of this query structure.
     * @return a collection of LabelSelections
     */
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    /**
     * This allows re-arranging residues in hits so they have the same order as the query.
     * @return an array tracks how residues were swapped
     */
    public List<Integer> getResidueIndexSwaps() {
        return residueIndexSwaps;
    }
}
