package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.core.HitScorer;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.TargetAssembler;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.motif.InvertedIndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.AssemblyInformation;
import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StateRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A container keeping track of paths in a target structure. Used to lazily load structures: Basically, wraps
 * {@link Structure} instances but oftentimes we don't need to load a structure at all (i.e. if there are no valid
 * paths).
 * <p>
 * Each instances represents one structure in the archive in which a motif could occur. It builds up all valid paths and
 * omits stale paths as greedily as possible. Realized with help of a {@link TargetAssembler}.
 * For efficiency, one target structures handles all potential paths in a structure.
 * <p>
 * The constructor and {@link TargetStructure#consume(InvertedIndexResiduePairIdentifier[], Overlap[])} iteratively builds up paths
 * until all paths are either ruled out or sufficient resemblance of the query motif is observed.
 */
public class TargetStructure {
    private final String structureIdentifier;
    // non-final fields to achieve the lazy behavior - tread lightly
    private List<InvertedIndexResiduePairIdentifier[]> paths;

    /**
     * Construct a target structure instance.
     * @param structureIdentifier its identifier
     * @param residuePairIdentifiers all first-generation residue pairs
     */
    public TargetStructure(String structureIdentifier, InvertedIndexResiduePairIdentifier[] residuePairIdentifiers) {
        this.structureIdentifier = structureIdentifier;
        // each target identifier is the first step of a potential path in this target structure
        // we use an ArrayList because for subsequent iterations we don't know the size ahead of time
        this.paths = new ArrayList<>(residuePairIdentifiers.length);
        for (InvertedIndexResiduePairIdentifier residuePairIdentifier : residuePairIdentifiers) {
            paths.add(new InvertedIndexResiduePairIdentifier[] { residuePairIdentifier });
        }
    }

    /**
     * Reports the number currently valid paths.
     * @return the number of valid paths in this structure
     */
    public int getNumberOfValidPaths() {
        return paths.size();
    }

    /**
     * The identifier of this structure
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Consumes a new 'generation' of residue pair identifiers which potentially resemble valid extensions to the
     * registered paths.
     * @param residuePairIdentifiers all words returned for particular residue pair descriptor
     * @param overlapProfile query motif overlap profile - needed to ensure compatibility
     * @return true if this target still contains at least one valid path
     */
    public boolean consume(InvertedIndexResiduePairIdentifier[] residuePairIdentifiers, Overlap[] overlapProfile) {
        List<InvertedIndexResiduePairIdentifier[]> extendedPaths = new ArrayList<>();

        // for each possibly extending candidate:
        for (InvertedIndexResiduePairIdentifier candidateResiduePairIdentifier : residuePairIdentifiers) {
            // form cartesian product with each possible path to extend:
            p:
            for (InvertedIndexResiduePairIdentifier[] path : paths) {
                // this path must allow for the same overlap profile as query
                for (int k = 0; k < overlapProfile.length; k++) {
                    InvertedIndexResiduePairIdentifier previousResiduePairIdentifier = path[k];

                    Overlap queryOverlap = overlapProfile[k];
                    if (!queryOverlap.test(previousResiduePairIdentifier, candidateResiduePairIdentifier)) {
                        continue p;
                    }
                }

                // if loop didn't break: residuePairIdentifier is valid extension of this path: propagate to next
                // generation
                InvertedIndexResiduePairIdentifier[] extendedPath = Arrays.copyOf(path, path.length + 1);
                extendedPath[path.length] = candidateResiduePairIdentifier;
                extendedPaths.add(extendedPath);
            }
        }

        paths = extendedPaths;
        // return true when a valid extension for the path was observed
        return !paths.isEmpty();
    }

    /**
     * Traverses all paths of this structure. A path is ordered collection of residues - they are provided in an order
     * that matches the query motif. This ensures a bidirectional mapping between query and potential hit. Implicitly,
     * this causes the structure to be parsed. Also, this method is supposed to be called once and results to be
     * consumed directly.
     * @param residueIndexSwaps how residues were rearranged
     * @param structure provides assembly information
     * @param hitScorer the hit scorer
     * @param stateRepository provides prepared assembly information
     * @param undefinedAssemblies allow hits without assembly?
     * @return a stream of lists containing residues (in correspondence with the query)
     */
    public Stream<Hit> paths(List<Integer> residueIndexSwaps, Structure structure, HitScorer hitScorer, StateRepository stateRepository, boolean undefinedAssemblies) {
        return paths.stream().flatMap(p -> createHits(p, residueIndexSwaps, structure, hitScorer, stateRepository, undefinedAssemblies));
    }

    private Stream<Hit> createHits(ResiduePairIdentifier[] identifiers, List<Integer> residueIndexSwaps, Structure structure, HitScorer hitScorer, StateRepository stateRepository, boolean undefinedAssemblies) {
        List<IndexSelection> indexSelections = orderIndexSelections(identifiers, residueIndexSwaps);
        List<LabelSelection> labelSelections = indexSelections.stream()
                .map(indexSelection -> {
                    LabelSelection labelSelection = structure.getLabelSelection(indexSelection.getIndex());
                    return new LabelSelection(labelSelection.getLabelAsymId(), indexSelection.getStructOperId(), labelSelection.getLabelSeqId());
                })
                .collect(Collectors.toList());

        // determine all assembly ids that this collection of label selections appears in
        int residueCount = labelSelections.size();
        // this is the inverted mapping from opers to assemblies that contain this expression - can't use structure map here
        Map<String, Set<String>> assemblyMap = stateRepository.selectAssemblyMap(structureIdentifier);
        Map<String, Long> assemblyCounts;

        if (assemblyMap.isEmpty()) {
            // only allow hits in 'default'/fallback assembly if flag active and actually no information present
            if (!undefinedAssemblies) {
                return Stream.empty();
            }

            assemblyCounts = Map.of(AssemblyInformation.UNKNOWN_ASSEMBLY_IDENTIFIER, (long) residueCount);
        } else {
            assemblyCounts = labelSelections.stream()
                    .map(labelSelection -> labelSelection.getLabelAsymId() + "_" + labelSelection.getStructOperId())
                    .map(assemblyMap::get)
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        }

        return assemblyCounts.entrySet()
                .stream()
                // this assembly must be valid for all residues
                .filter(entry -> entry.getValue() == residueCount)
                .map(entry -> {
                    ResidueType[] residueTypes = new ResidueType[residueCount];
                    @SuppressWarnings("unchecked")
                    Map<LabelAtomId, float[]>[] residues = new Map[residueCount];

                    for (int i = 0; i < residueCount; i++) {
                        int index = indexSelections.get(i).getIndex();
                        residueTypes[i] = structure.getResidueType(index);
                        residues[i] = structure.manifestResidue(index);
                    }

                    AlignmentResult alignmentResult = hitScorer.alignToReference(Arrays.asList(residues));

                    return new Hit(structureIdentifier,
                            entry.getKey(),
                            labelSelections,
                            Arrays.asList(residueTypes),
                            alignmentResult.getRootMeanSquareDeviation(),
                            alignmentResult.getTransformation());
                });
    }

    private List<IndexSelection> orderIndexSelections(ResiduePairIdentifier[] identifiers, List<Integer> residueIndexSwaps) {
        try {
            // ensure correct 'human-readable' order of residues
            List<IndexSelection> shuffledIndexSelections = Arrays.stream(identifiers)
                    .flatMap(ResiduePairIdentifier::indexSelections)
                    .distinct()
                    .collect(Collectors.toList());

            return residueIndexSwaps.stream()
                    .map(shuffledIndexSelections::get)
                    .collect(Collectors.toList());
        } catch (IndexOutOfBoundsException e) {
            // this indicates that fewer residues are present in the result than specified by the query
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }
    }
}
