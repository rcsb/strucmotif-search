package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.motif.*;
import org.rcsb.strucmotif.math.Algebra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.*;

public class ResidueGraph {
    private static final Logger logger = LoggerFactory.getLogger(ResidueGraph.class);
    private final Structure structure;
    private Map<Long, Short> data;
    private final int selectionCount;
    private final int residueCount;
    private final int pairingCount;

    public enum ResidueGraphMode {
        /**
         * Only report contacts of deposited coordinates.
         */
        DEPOSITED,
        /**
         * Reports contacts between deposited coordinates as well as all residues that are in contact with the deposited
         * chain(s). This is a superset of `DEPOSITED`.
         */
        RESIDUES_IN_CONTACT,
        /**
         * Report contacts between deposited coordinates as well as all chains that are in contact with the deposited
         * chain(s). This is a superset of `RESIDUES_IN_CONTACT`.
         */
        CHAINS_IN_CONTACT,
        /**
         * Absolutely everything.
         */
        ALL,
        /**
         * Report contacts within a specific assembly.
         */
        ASSEMBLY,
        /**
         * Report all contacts for a list of {@link LabelSelection} values.
         */
        SELECTION
    }

    public record ResidueGraphOptions(ResidueGraphMode mode, String assemblyIdentifier,
                                             List<Map<LabelAtomId, float[]>> residues,
                                             List<LabelSelection> selections) {
        /**
         * Only report contacts of deposited coordinates.
         *
         * @return the corresponding options
         */
        public static ResidueGraphOptions deposited() {
            return new ResidueGraphOptions(ResidueGraphMode.DEPOSITED, null, null, null);
        }

        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         * Only includes residues in contact.
         *
         * @return the corresponding options
         */
        public static ResidueGraphOptions residuesInContact() {
            return new ResidueGraphOptions(ResidueGraphMode.RESIDUES_IN_CONTACT, null, null, null);
        }

        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         * Considers all residues in contact and expands this selection to the whole chain.
         *
         * @return the corresponding options
         */
        public static ResidueGraphOptions chainsInContact() {
            return new ResidueGraphOptions(ResidueGraphMode.CHAINS_IN_CONTACT, null, null, null);
        }

        /**
         * Index everything, this will bring in a lot of redundancy when many transformed copies of a chain are present.
         * @return the corresponding option
         */
        public static ResidueGraphOptions all() {
            return new ResidueGraphOptions(ResidueGraphMode.ALL, null, null, null);
        }

        /**
         * Only report contacts of deposited coordinates.
         *
         * @param assemblyIdentifier which assembly to consider?
         * @return the corresponding options
         */
        public static ResidueGraphOptions assembly(String assemblyIdentifier) {
            return new ResidueGraphOptions(ResidueGraphMode.ASSEMBLY, assemblyIdentifier, null, null);
        }

        /**
         * Report all contacts, regardless of assembly or applied transformation. This makes only sense when the graph
         * is computed on a substructure, defined by a number of {@link LabelSelection}.
         *
         * @param residues   the manifested residues of interest
         * @param selections the respective selection
         * @return the corresponding options
         */
        public static ResidueGraphOptions selection(List<Map<LabelAtomId, float[]>> residues, List<LabelSelection> selections) {
            return new ResidueGraphOptions(ResidueGraphMode.SELECTION, null, residues, selections);
        }
    }

    public ResidueGraph(Structure structure, StrucmotifConfig strucmotifConfig, ResidueGraphOptions options) {
        this.structure = structure;

        ResidueVectors residueVectors = populateResidueVectors(structure, options);

        this.selectionCount = options.mode == ResidueGraphMode.SELECTION ? options.selections.size() : (residueVectors.backboneVectors.length / 3);
        this.residueCount = residueVectors.backboneVectors.length / 3;
        this.pairingCount = fillResidueGrid(structure, residueVectors, strucmotifConfig.getSquaredDistanceCutoff(), options);
    }

    record ResidueVectors(int[] residueIndices, float[] backboneVectors, float[] sideChainVectors, float[] normalVectors) {}

    private ResidueVectors populateResidueVectors(Structure structure, ResidueGraphOptions options) {
        int valid = 0;
        int bound = options.mode == ResidueGraphMode.SELECTION ? options.selections.size() : structure.getInstancedResidueCount();
        int[] residueIndices = new int[bound];
        float[] backboneVectors = new float[3 * bound];
        float[] sideChainVectors = new float[3 * bound];
        float[] normalVectors = new float[3 * bound];

        if (options.mode == ResidueGraphMode.SELECTION) {
            for (int i = 0; i < residueIndices.length; i++) {
                LabelSelection labelSelection = options.selections.get(i);
                // this may not be the actual residueIndex retrieved during search, but it doesn't matter
                int residueIndex = structure.getResidueIndex(labelSelection);
                residueIndices[i] = residueIndex;
            }
        } else if (options.mode == ResidueGraphMode.DEPOSITED) {
            int j = 0;
            for (int i = 0; i < structure.getInstancedResidueCount(); i++) {
                if (!structure.getTransformationIdentifier(i).equals("1")) {
                    continue;
                }
                residueIndices[j++] = i;
            }
            residueIndices = Arrays.copyOf(residueIndices, j);
        } else {
            for (int i = 0; i < structure.getInstancedResidueCount(); i++) {
                residueIndices[i] = i;
            }
        }

        for (int i = 0; i < residueIndices.length; i++) {
            int residueIndex = residueIndices[i];
            ResidueType residueType = structure.getResidueType(residueIndex);
            Map<LabelAtomId, float[]> residue = structure.manifestResidue(residueIndex);

            float[] backbone = getBackboneCoords(residue);
            if (backbone == null) {
                continue;
            }
            float[] sideChain = residueType == ResidueType.GLYCINE ? getVirtualCB(residue) : getSideChainCoords(residue);
            if (sideChain == null) {
                continue;
            }

            residueIndices[valid] = residueIndex;
            int insertionPoint = valid * 3;
            System.arraycopy(backbone, 0, backboneVectors, insertionPoint, 3);
            System.arraycopy(sideChain, 0, sideChainVectors, insertionPoint, 3);
            float[] normalVector = normalVector(backbone, sideChain);
            System.arraycopy(normalVector, 0, normalVectors, insertionPoint, 3);
            valid++;
        }

        // compact arrays
        residueIndices = Arrays.copyOf(residueIndices, valid);
        backboneVectors = Arrays.copyOf(backboneVectors, valid * 3);
        sideChainVectors = Arrays.copyOf(sideChainVectors, valid * 3);
        normalVectors = Arrays.copyOf(normalVectors, valid * 3);

        return new ResidueVectors(residueIndices, backboneVectors, sideChainVectors, normalVectors);
    }

    private int fillResidueGrid(Structure structure, ResidueVectors residueVectors, float squaredDistanceCutoff, ResidueGraphOptions options) {
        // temporary ResidueGrid for efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(residueVectors, squaredDistanceCutoff);
        int[] residueIndices = residueVectors.residueIndices;
        ResidueGraphMode mode = options.mode;
        // factored out to not compute twice, possible to not add undesired residues to the grid in the first place?
        List<ResidueGrid.ResidueContact> contacts = residueGrid.getIndicesContacts();

        List<Set<String>> assemblyGrouping = new ArrayList<>();
        for (String assemblyIdentifier : structure.getAssemblyIdentifiers()) {
            Set<String> referencedTransformationIdentifiers = new HashSet<>();
            String[] referencedChainInstances = structure.getReferencedChainInstances(assemblyIdentifier);
            for (int i = 0; i < referencedChainInstances.length - 1; i = i + 2) {
                referencedTransformationIdentifiers.add(referencedChainInstances[i] + "_" + referencedChainInstances[i + 1]);
            }
            assemblyGrouping.add(referencedTransformationIdentifiers);
        }

        // track the first occurrence of each chain and consider that 'deposited'
        Set<String> acceptedChains = new HashSet<>();
        Set<String> acceptedTransformationIdentifiers = new HashSet<>();
        if (mode == ResidueGraphMode.DEPOSITED || mode == ResidueGraphMode.RESIDUES_IN_CONTACT || mode == ResidueGraphMode.CHAINS_IN_CONTACT) {
            for (String assemblyIdentifier : structure.getAssemblyIdentifiers()) {
                String[] referencedChainInstances = structure.getReferencedChainInstances(assemblyIdentifier);
                for (int i = 0; i < referencedChainInstances.length - 1; i = i + 2) {
                    String chainIdentifier = referencedChainInstances[i];
                    if (!acceptedChains.contains(chainIdentifier)) {
                        acceptedChains.add(chainIdentifier);
                        acceptedTransformationIdentifiers.add(chainIdentifier + "_" + referencedChainInstances[i + 1]);
                    }
                }
            }

            // need 2nd pass to find all chain instances in contact with instances accepted in the 1st pass
            if (mode == ResidueGraphMode.CHAINS_IN_CONTACT) {
                Set<String> additions = new HashSet<>();
                for (ResidueGrid.ResidueContact residueContact : contacts) {
                    // must contain 'dominant' chain instance
                    int residueIndex1 = residueIndices[residueContact.i()];
                    String transformationIdentifier1 = structure.getLabelAsymId(residueIndex1) + "_" + structure.getTransformationIdentifier(residueIndex1);
                    if (!acceptedTransformationIdentifiers.contains(transformationIdentifier1)) {
                        continue;
                    }

                    // all tracked other residues are known to be below the cutoff at this point
                    int residueIndex2 = residueIndices[residueContact.j()];
                    String transformationIdentifier2 = structure.getLabelAsymId(residueIndex2) + "_" + structure.getTransformationIdentifier(residueIndex2);
                    additions.add(transformationIdentifier2);
                }
                acceptedTransformationIdentifiers.addAll(additions);
            }
        } else if (mode == ResidueGraphMode.ASSEMBLY) {
            String[] requestedChains = structure.getReferencedChainInstances(options.assemblyIdentifier);
            for (int i = 0; i < requestedChains.length - 1; i = i + 2) {
                acceptedTransformationIdentifiers.add(requestedChains[i] + "_" + requestedChains[i + 1]);
            }
        } else if (mode == ResidueGraphMode.ALL) {
            for (String assemblyIdentifier : structure.getAssemblyIdentifiers()) {
                String[] referencedChainInstances = structure.getReferencedChainInstances(assemblyIdentifier);
                for (int i = 0; i < referencedChainInstances.length - 1; i = i + 2) {
                    acceptedTransformationIdentifiers.add(referencedChainInstances[i] + "_" + referencedChainInstances[i + 1]);
                }
            }
        }

        int contactCount = 0;
        this.data = new LinkedHashMap<>();
        for (ResidueGrid.ResidueContact residueContact : contacts) {
            int i = residueContact.i();
            int j = residueContact.j();
            int residueIndex1 = residueIndices[i];
            int residueIndex2 = residueIndices[j];

            String transformationIdentifier1 = structure.getLabelAsymId(residueIndex1) + "_" + structure.getTransformationIdentifier(residueIndex1);
            String transformationIdentifier2 = structure.getLabelAsymId(residueIndex2) + "_" + structure.getTransformationIdentifier(residueIndex2);
            if (mode != ResidueGraphMode.SELECTION) {
                if (!acceptedTransformationIdentifiers.contains(transformationIdentifier1)) {
                    continue;
                }

                if (mode == ResidueGraphMode.DEPOSITED || mode == ResidueGraphMode.ASSEMBLY) {
                    if (!acceptedTransformationIdentifiers.contains(transformationIdentifier2)) {
                        continue;
                    }
                }
            }

            // ensure both chain instances are part of the same assembly
            if (assemblyGrouping.stream().noneMatch(group -> group.contains(transformationIdentifier1) && group.contains(transformationIdentifier2))) {
                continue;
            }

            DistanceType backboneDistance = DistanceType.ofDistance(residueContact.distance());
            DistanceType sideChainDistance = DistanceType.ofDistance((float) Math.sqrt(distanceSquared3d(residueVectors.sideChainVectors, i, j)));
            AngleType angle = AngleType.ofAngle(angle(residueVectors.normalVectors, i, j));

            // jam all values into a single short
            long key = ResiduePairIdentifier.encodeIdentifier(residueIndex1, residueIndex2);
            short value = ResiduePairDescriptor.encodeDescriptor(backboneDistance, sideChainDistance, angle);
            data.put(key, value);

            contactCount++;
        }
        return contactCount;
    }

    public int getSelectionCount() {
        return selectionCount;
    }

    public int getResidueCount() {
        return residueCount;
    }

    public int getPairingCount() {
        return pairingCount;
    }

    public LongStream pairingsSequential() {
        return data.keySet().stream().mapToLong(l -> l);
    }

    public LongStream pairingsParallel() {
        return data.keySet().parallelStream().mapToLong(l -> l);
    }

    public Stream<ResiduePairOccurrence> residuePairOccurrencesSequential() {
        return pairingsSequential()
                .mapToObj(this::createResiduePairOccurrence);
    }

    public Stream<ResiduePairOccurrence> residuePairOccurrencesParallel() {
        return pairingsParallel()
                .mapToObj(this::createResiduePairOccurrence);
    }

    private ResiduePairOccurrence createResiduePairOccurrence(long residuePairIdentifier) {
        int residueIndex1 = ResiduePairIdentifier.getResidueIndex1(residuePairIdentifier);
        int residueIndex2 = ResiduePairIdentifier.getResidueIndex2(residuePairIdentifier);
        ResidueType residueType1 = structure.getResidueType(residueIndex1);
        ResidueType residueType2 = structure.getResidueType(residueIndex2);

        short value = data.get(residuePairIdentifier);
        DistanceType backboneDistance = ResiduePairDescriptor.getBackboneDistance(value);
        DistanceType sideChainDistance = ResiduePairDescriptor.getSideChainDistance(value);
        AngleType angle = ResiduePairDescriptor.getAngle(value);

        // check if pair needs to be flipped to follow contract
        if (residueType1.getInternalCode().compareTo(residueType2.getInternalCode()) > 0) {
            return new ResiduePairOccurrence(residueIndex2, residueIndex1, residueType2, residueType1, backboneDistance, sideChainDistance, angle);
        } else {
            return new ResiduePairOccurrence(residueIndex1, residueIndex2, residueType1, residueType2, backboneDistance, sideChainDistance, angle);
        }
    }

    private static float[] getBackboneCoords(Map<LabelAtomId, float[]> residue) {
        if (residue.containsKey(LabelAtomId.CA)) {
            return residue.get(LabelAtomId.CA);
        }
        if (residue.containsKey(LabelAtomId.C4_PRIME)) {
            return residue.get(LabelAtomId.C4_PRIME);
        }
        return null;
    }

    private static float[] getSideChainCoords(Map<LabelAtomId, float[]> residue) {
        if (residue.containsKey(LabelAtomId.CB)) {
            return residue.get(LabelAtomId.CB);
        }
        if (residue.containsKey(LabelAtomId.C1_PRIME)) {
            return residue.get(LabelAtomId.C1_PRIME);
        }
        return null;
    }

    private static float[] normalVector(float[] a, float[] b) {
        float[] ba = new float[3];
        subtract3d(ba, b, a);
        normalize3d(ba, ba);
        return ba;
    }

    // already centered coordinates to save operations
    private static final List<float[]> REFERENCE_BACKBONE = List.of(new float[]{-0.698f, 0.184f, 1.008f}, // N
            new float[]{0.525f, 0.109f, 0.200f}, // CA
            new float[]{0.174f, -0.292f, -1.208f}); // C
    private static final float[] REFERENCE_CB = new float[]{1.472f, -0.929f, 0.804f};
    private static final float[] REFERENCE_CENTROID = new float[3];

    static float[] getVirtualCB(Map<LabelAtomId, float[]> residue) {
        float[] n = residue.get(LabelAtomId.N);
        float[] ca = residue.get(LabelAtomId.CA);
        float[] c = residue.get(LabelAtomId.C);
        if (n == null || ca == null || c == null) {
            return null;
        }

        List<float[]> coords = List.of(n, ca, c);
        float[] v = Algebra.centroid3d(coords);

        float[] transformation = QuaternionAlignmentService.align(coords, v, REFERENCE_BACKBONE, REFERENCE_CENTROID).first();
        Algebra.multiply4d(v, transformation, REFERENCE_CB);
        return v;
    }

    static float distanceSquared3d(float[] vectors, int i, int j) {
        i *= 3;
        j *= 3;
        float dx = vectors[i] - vectors[j];
        float dy = vectors[i + 1] - vectors[j + 1];
        float dz = vectors[i + 2] - vectors[j + 2];

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Compute the angle between 2 vectors. Must be normal vectors (will not be normalized in this call).
     *
     * @param vectors data
     * @param i       index of 1st vector
     * @param j       index of 2nd vector
     * @return the angle in degrees
     */
    static float angle(float[] vectors, int i, int j) {
        i *= 3;
        j *= 3;
        // be careful, normally dot product has to be divided by norm of v1 and v2
        float vDot = vectors[i] * vectors[j] + vectors[i + 1] * vectors[j + 1] + vectors[i + 2] * vectors[j + 2];
        return (float) Math.toDegrees(Math.acos(capToInterval(-1, vDot, 1)));
    }

    /**
     * Checks whether the graph described by these residues is connected. Not optimized and only intended to check
     * validity of query motifs.
     *
     * @return false if there are residues that aren't reachable from another residue or if the graph has no nodes
     */
    public boolean isConnected() {
        long start = System.nanoTime();
        if (selectionCount == 0) {
            return false;
        }

        Set<Integer> visited = new HashSet<>();
        Set<Pair<Integer, Integer>> edges = residuePairOccurrencesSequential()
                .map(o -> new Pair<>(o.getResidueIndex1(), o.getResidueIndex2()))
                .collect(Collectors.toSet());

        if (data.isEmpty()) {
            return false;
        }

        int random = ResiduePairIdentifier.getResidueIndex1(data.keySet().iterator().next());
        dfs(random, edges, visited);

        logger.debug("Checked for connectedness in {} ms", (System.nanoTime() - start) * 0.001 * 0.001);
        return visited.size() == selectionCount;
    }

    private void dfs(int currentNode, Set<Pair<Integer, Integer>> edges, Set<Integer> visited) {
        visited.add(currentNode);

        for (Pair<Integer, Integer> edge : edges) {
            int node1 = edge.first();
            int node2 = edge.second();
            if (currentNode != node1 && currentNode != node2) {
                continue;
            }

            int neighbor = node1 == currentNode ? node2 : node1;
            if (!visited.contains(neighbor)) {
                dfs(neighbor, edges, visited);
            }
        }
    }
}
