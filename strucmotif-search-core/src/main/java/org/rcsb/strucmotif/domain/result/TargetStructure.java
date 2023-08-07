package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.core.HitScorer;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.TargetAssembler;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
 * The constructor and {@link TargetStructure#consume(int[], Overlap[])} iteratively builds up paths
 * until all paths are either ruled out or sufficient resemblance of the query motif is observed.
 */
public class TargetStructure {
    private final int structureIndex;
    // non-final fields to achieve the lazy behavior - tread lightly
    private List<int[]> paths;

    /**
     * Construct a target structure instance.
     * @param structureIndex its identifier
     * @param residuePairIdentifiers all first-generation residue pairs
     */
    public TargetStructure(int structureIndex, int[] residuePairIdentifiers) {
        this.structureIndex = structureIndex;
        // each target identifier is the first step of a potential path in this target structure
        // we use an ArrayList because for subsequent iterations we don't know the size ahead of time
        this.paths = new ArrayList<>(residuePairIdentifiers.length / 2);
        for (int i = 0; i < residuePairIdentifiers.length - 1; i = i + 2) {
            paths.add(new int[] { residuePairIdentifiers[i], residuePairIdentifiers[i + 1] });
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
     * The index of this structure
     * @return an int
     */
    public int getStructureIndex() {
        return structureIndex;
    }

    /**
     * Consumes a new 'generation' of residue pair identifiers which potentially resemble valid extensions to the
     * registered paths.
     * @param residuePairIdentifiers all words returned for particular residue pair descriptor
     * @param overlapProfile query motif overlap profile - needed to ensure compatibility
     * @return true if this target still contains at least one valid path
     */
    public boolean consume(int[] residuePairIdentifiers, Overlap[] overlapProfile) {
        List<int[]> extendedPaths = new ArrayList<>();

        // for each possibly extending candidate:
        for (int i = 0; i < residuePairIdentifiers.length - 1; i = i + 2) {
            int candidateResidueIndex1 = residuePairIdentifiers[i];
            int candidateResidueIndex2 = residuePairIdentifiers[i  +1];
            // form cartesian product with each possible path to extend:
            p:
            for (int[] path : paths) {
                // this path must allow for the same overlap profile as query
                for (int k = 0; k < overlapProfile.length; k++) {
                    int previousResidueIndex1 = path[2 * k];
                    int previousResidueIndex2 = path[2 * k + 1];

                    Overlap queryOverlap = overlapProfile[k];
                    Overlap candidateOverlap = Overlap.ofResiduePairIdentifiers(previousResidueIndex1, previousResidueIndex2, candidateResidueIndex1, candidateResidueIndex2);
                    if (queryOverlap != candidateOverlap) {
                        continue p;
                    }
                }

                // if loop didn't break: indices are valid extension of this path: propagate to next generation
                int[] extendedPath = Arrays.copyOf(path, path.length + 2);
                extendedPath[path.length] = candidateResidueIndex1;
                extendedPath[path.length + 1] = candidateResidueIndex2;
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
     * @param structure the structure data
     * @param structureIdentifier the structureIdentifier
     * @param hitScorer the hit scorer
     * @param rmsdCutoff what hits to ignore
     * @return a stream of lists containing residues (in correspondence with the query)
     */
    public Stream<StructureHit> paths(int[] residueIndexSwaps, Structure structure, String structureIdentifier, HitScorer hitScorer, float rmsdCutoff) {
        return paths.stream().flatMap(p -> createHits(p, residueIndexSwaps, structure, structureIdentifier, hitScorer, rmsdCutoff));
    }

    private Stream<StructureHit> createHits(int[] residuePairIdentifiers, int[] residueIndexSwaps, Structure structure, String structureIdentifier, HitScorer hitScorer, float rmsdCutoff) {
        int[] residueIndices = orderResidueIndices(residuePairIdentifiers, residueIndexSwaps);
        List<LabelSelection> labelSelections = IntStream.of(residueIndices)
                .mapToObj(structure::getLabelSelection)
                .collect(Collectors.toList());

        // determine all assembly ids that these selections appears in
        int residueCount = labelSelections.size();
        Map<String, Long> assemblyCounts = IntStream.of(residueIndices)
                .mapToObj(structure::getAssemblyIdentifier)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return assemblyCounts.entrySet()
                .stream()
                // this assembly must be valid for all residues
                .filter(entry -> entry.getValue() == residueCount)
                .map(entry -> {
                    ResidueType[] residueTypes = new ResidueType[residueCount];
                    @SuppressWarnings("unchecked")
                    Map<LabelAtomId, float[]>[] residues = new Map[residueCount];

                    for (int i = 0; i < residueCount; i++) {
                        int residueIndex = residueIndices[i];
                        residueTypes[i] = structure.getResidueType(residueIndex);
                        residues[i] = structure.manifestResidue(residueIndex);
                    }

                    AlignmentResult alignmentResult = hitScorer.alignToReference(Arrays.asList(residues));
                    // filter away high-RMSD hits
                    if (alignmentResult.rootMeanSquareDeviation() >= rmsdCutoff) {
                        return null;
                    }

                    return new StructureHit(structureIdentifier,
                            entry.getKey(),
                            labelSelections,
                            Arrays.asList(residueTypes),
                            alignmentResult.rootMeanSquareDeviation(),
                            alignmentResult.transformation());
                })
                .filter(Objects::nonNull);
    }

    private int[] orderResidueIndices(int[] identifiers, int[] residueIndexSwaps) {
        try {
            // ensure correct 'human-readable' order of residues
            int[] shuffledResidueIndices = Arrays.stream(identifiers).distinct().toArray();

            return IntStream.of(residueIndexSwaps)
                    .map(i -> shuffledResidueIndices[i])
                    .toArray();
        } catch (IndexOutOfBoundsException e) {
            // this indicates that fewer residues are present in the result than specified by the query
            throw new IllegalQueryDefinitionException("Query violates distance threshold");
        }
    }
}
