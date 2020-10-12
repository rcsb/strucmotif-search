package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.Overlap;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.persistence.SelectionMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A container keeping track of paths in a target structure. Used to lazily load structures: Basically, wraps
 * {@link Structure} instances but oftentimes we don't need to load a structure at all (i.e. if there are no valid
 * paths).
 * <p>
 * Each instances represents one structure in the archive in which a motif could occur. It builds up all valid paths and
 * omits stale paths as greedily as possible. Realized with help of a {@link org.rcsb.strucmotif.core.TargetAssembler}.
 * For efficiency, one target structures handles all potential paths in a structure.
 * <p>
 * The constructor and {@link TargetStructure#consume(ResiduePairIdentifier[], Overlap[])} iteratively builds up paths
 * until all paths are either ruled out or sufficient resemblance of the query motif is observed.
 */
public class TargetStructure {
    private final SelectionMapper<IndexSelection, LabelSelection> selectionMapper;
    private final StructureIdentifier structureIdentifier;
    private final double scoreCutoff;

    /*
    We use some non-final fields to achieve the lazy behavior. Tread lightly.
     */
    private List<ResiduePairIdentifier[]> paths;

    public TargetStructure(StructureIdentifier structureIdentifier, ResiduePairIdentifier[] residuePairIdentifiers, SelectionMapper<IndexSelection, LabelSelection> selectionMapper, double scoreCutoff) {
        this.selectionMapper = selectionMapper;
        this.structureIdentifier = structureIdentifier;
        this.scoreCutoff = scoreCutoff;
        // each target identifier is the first step of a potential path in this target structure
        // we use an ArrayList because for subsequent iterations we don't know the size ahead of time
        this.paths = new ArrayList<>(residuePairIdentifiers.length);
        for (ResiduePairIdentifier residuePairIdentifier : residuePairIdentifiers) {
            paths.add(new ResiduePairIdentifier[] { residuePairIdentifier });
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
     * @return a structure identifier instance
     */
    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Consumes a new 'generation' of residue pair identifiers which potentially resemble valid extensions to the
     * registered paths.
     * @param residuePairIdentifiers all words returned for particular residue pair descriptor
     * @param overlapProfile query motif overlap profile - needed to ensure compatibility
     * @return true if this target still contains at least one valid path
     */
    public boolean consume(ResiduePairIdentifier[] residuePairIdentifiers, Overlap[] overlapProfile) {
        List<ResiduePairIdentifier[]> extendedPaths = new ArrayList<>();

        // for each possibly extending candidate:
        for (ResiduePairIdentifier candidateResiduePairIdentifier : residuePairIdentifiers) {
            // form cartesian product with each possible path to extend:
            p:
            for (ResiduePairIdentifier[] path : paths) {
                // this path must allow for the same overlap profile as query
                for (int k = 0; k < overlapProfile.length; k++) {
                    ResiduePairIdentifier previousResiduePairIdentifier = path[k];

                    Overlap queryOverlap = overlapProfile[k];
                    Overlap targetOverlap = Overlap.ofResiduePairIdentifiers(previousResiduePairIdentifier, candidateResiduePairIdentifier);
                    if (queryOverlap != targetOverlap) {
                        continue p;
                    }
                }

                // if loop didn't break: residuePairIdentifier is valid extension of this path: propagate to next
                // generation
                ResiduePairIdentifier[] extendedPath = Arrays.copyOf(path, path.length + 1);
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
     * @return a stream of lists containing residues (in correspondence with the query)
     */
    public Stream<SimpleHit> paths() {
        try {
            return paths.stream()
                    .map(this::createSimpleHit);
        } catch (Exception e) {
            // rarely happens when the index references a file not present in the selection-db
            // this is caused by entries that were removed from the archive but can be fixed by removing old entries from archive/index/componentDB
            return Stream.empty();
        }
    }

    private SimpleHit createSimpleHit(ResiduePairIdentifier[] identifiers) {
        int[] partial = new int[] { 0, 0, 0 };
        for (ResiduePairIdentifier identifier : identifiers) {
            partial[0] += identifier.getScore().getBackboneScore();
            partial[1] += identifier.getScore().getSideChainScore();
            partial[2] += identifier.getScore().getAngleScore();
        }
        GeometricDescriptorScore score = new GeometricDescriptorScore(partial[0], partial[1], partial[2]);
        // TODO revisit this later
        if (score.value() > scoreCutoff) {
            return null;
        }

        List<IndexSelection> indexSelections = Arrays.stream(identifiers)
                .flatMap(ResiduePairIdentifier::indexSelections)
                .distinct()
                .collect(Collectors.toList());
        return new SimpleHit(structureIdentifier,
                selectionMapper.select(structureIdentifier, indexSelections),
                score);
    }
}
