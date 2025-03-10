package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.QueryExecutionException;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * A query structure when searching for other structures.
 */
public class StructureQueryStructure implements QueryStructure {
    private final String structureIdentifier;
    private final Structure structure;
    private final List<Integer> residueIndices;
    private final List<Map<LabelAtomId, float[]>> residues;
    private final List<ResiduePairOccurrence> residuePairOccurrences;
    private final List<Long> residuePairIdentifiers;
    private final List<Integer> residuePairDescriptors;
    private final List<Integer> residueIndexSwaps;

    /**
     * Construct a structure query structure.
     * @param structureIdentifier the identifier
     * @param structure the structure
     * @param originalLabelSelections residues to select
     * @param originalResidues coordinates of residues
     * @param residuePairOccurrences contacts between residues
     */
    public StructureQueryStructure(String structureIdentifier, Structure structure, List<LabelSelection> originalLabelSelections, List<Map<LabelAtomId, float[]>> originalResidues, List<ResiduePairOccurrence> residuePairOccurrences) {
        this.structureIdentifier = structureIdentifier;
        this.structure = structure;
        if (residuePairOccurrences.isEmpty()) {
            throw new IllegalQueryDefinitionException("Did not find any residue pairs in structure - check query definition");
        }

        // sort occurrences to ensure that no dangling words are encountered during path assembly
        List<ResiduePairOccurrence> connectedResiduePairs = getPathOfConnectedResiduePairs(residuePairOccurrences);

        this.residuePairOccurrences = connectedResiduePairs;
        this.residuePairIdentifiers = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResiduePairIdentifier)
                .toList();
        this.residuePairDescriptors = connectedResiduePairs.stream()
                .map(ResiduePairOccurrence::getResiduePairDescriptor)
                .toList();

        // explode query into motifs and get entities by that - this provides the correct order of entities so that the
        // alignment routine does not have to care about finding correspondence
        this.residueIndices = residuePairIdentifiers.stream()
                .flatMap(i -> Stream.of(ResiduePairIdentifier.getResidueIndex1(i), ResiduePairIdentifier.getResidueIndex2(i)))
                .distinct()
                .toList();

        if (residueIndices.size() != originalResidues.size()) {
            // this indicates that fewer residues are present in the result than specified by the query
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }
        this.residues = originalResidues;
        this.residueIndexSwaps = resolveSwaps(originalLabelSelections);
    }

    private List<Integer> resolveSwaps(List<LabelSelection> originalLabelSelections) {
        for (String assemblyId : structure.getAssemblyIdentifiers()) {
            try {
                List<Integer> originalResidueIndices = originalLabelSelections.stream()
                        .map(l -> structure.getResidueIndex(assemblyId, l))
                        .toList();

                if (originalResidueIndices.contains(-1)) {
                    continue;
                }

                return originalResidueIndices.stream()
                        .map(residueIndices::indexOf)
                        .toList();
            } catch (NoSuchElementException e) {
                // safe as long as some other assembly allows resolving all residue indices
            }
        }

        throw new QueryExecutionException("Failed to resolve residue swaps, are all residues in the same assembly?");
    }

    /**
     * Determine a unique path through this structure which captures/passes all residue pairs. Each residue must be
     * present at least once.
     * @param residuePairOccurrences the collection of residue pair occurrences to process
     * @return a filtered collection of residue pair occurrences
     */
    private List<ResiduePairOccurrence> getPathOfConnectedResiduePairs(List<ResiduePairOccurrence> residuePairOccurrences) {
        List<ResiduePairOccurrence> sorted = ResiduePairOccurrence.sort(residuePairOccurrences);

        // assign 'random' word as start
        List<ResiduePairOccurrence> sparse = new ArrayList<>();
        sparse.add(sorted.remove(0));

        while (!sorted.isEmpty()) {
            for (int i = 0; i < sorted.size(); i++) {
                ResiduePairOccurrence candidateResiduePair = sorted.get(i);
                long candidateIdentifier = candidateResiduePair.getResiduePairIdentifier();
                // check for overlap with already accepted word
                if (sparse.stream().anyMatch(sortedResiduePair -> match(sortedResiduePair.getResiduePairIdentifier(), candidateIdentifier))) {
                    sparse.add(sorted.remove(i));
                    break;
                }
            }
        }

        return sparse;
    }

    /**
     * Check if two identifiers reference an overlapping index selection.
     * @param sortedWordResiduePairIdentifier reference
     * @param candidateIdentifier candidate
     * @return true if describing an overlapping selection
     */
    private boolean match(long sortedWordResiduePairIdentifier, long candidateIdentifier) {
        int r11 = ResiduePairIdentifier.getResidueIndex1(sortedWordResiduePairIdentifier);
        int r12 = ResiduePairIdentifier.getResidueIndex2(sortedWordResiduePairIdentifier);
        int r21 = ResiduePairIdentifier.getResidueIndex1(candidateIdentifier);
        int r22 = ResiduePairIdentifier.getResidueIndex2(candidateIdentifier);
        return r11 == r21 || r11 == r22 || r12 == r21 || r12 == r22;
    }

    @Override
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    @Override
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
    public List<Long> getResiduePairIdentifiers() {
        return residuePairIdentifiers;
    }

    /**
     * All word descriptors in this query structure.
     * @return a collection of word descriptors
     */
    public List<Integer> getResiduePairDescriptors() {
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
     * @return a collection of residue indices
     */
    public List<Integer> getResidueIndices() {
        return residueIndices;
    }

    /**
     * This allows re-arranging residues in hits, so they have the same order as the query.
     * @return an array tracks how residues were swapped
     */
    public List<Integer> getResidueIndexSwaps() {
        return residueIndexSwaps;
    }
}
