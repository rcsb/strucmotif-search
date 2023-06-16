package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.IndexSelectionResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.math.Algebra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.all;
import static org.rcsb.strucmotif.math.Algebra.*;

/**
 * Represents the pseudo-graph defined by all residues. Some tricks to efficiently report distances and angles between
 * residues.
 */
public class ResidueGraph {
    private static final Logger logger = LoggerFactory.getLogger(ResidueGraph.class);
    private final Structure structure;
    private final Map<IndexSelection, Map<IndexSelection, Float>> backboneDistances;
    private final Map<IndexSelection, Map<IndexSelection, Float>> sideChainDistances;
    private final Map<IndexSelection, Map<IndexSelection, Float>> angles;
    private final int selectionCount;
    private final int numberOfResidues;
    private final int numberOfPairings;

    /**
     * Different modes of how the residue graph can operate.
     */
    public enum ResidueGraphMode {
        /**
         * Only report contacts of deposited coordinates.
         */
        DEPOSITED,
        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         */
        DEPOSITED_AND_CONTACTS,
        /**
         * Report contacts within a specific assembly.
         */
        ASSEMBLY,
        /**
         * Report all contacts, regardless of assembly or applied transformation. This makes only sense when the graph
         * is computed on a substructure, defined by a number of {@link LabelSelection}.
         */
        ALL
    }

    /**
     * Modulate behavior of the residue graph.
     */
    public static class ResidueGraphOptions {
        final ResidueGraphMode mode;
        final String assemblyIdentifier;

        private ResidueGraphOptions(ResidueGraphMode mode, String assemblyIdentifier) {
            this.mode = mode;
            this.assemblyIdentifier = assemblyIdentifier;
        }

        /**
         * Only report contacts of deposited coordinates.
         * @return the corresponding options
         */
        public static ResidueGraphOptions deposited() {
            return new ResidueGraphOptions(ResidueGraphMode.DEPOSITED, null);
        }

        /**
         * Report contacts between deposited coordinates as well as deposited coordinates and a transformed partner.
         * @return the corresponding options
         */
        public static ResidueGraphOptions depositedAndContacts() {
            return new ResidueGraphOptions(ResidueGraphMode.DEPOSITED_AND_CONTACTS, null);
        }

        /**
         * Only report contacts of deposited coordinates.
         * @param assemblyIdentifier which assembly to consider?
         * @return the corresponding options
         */
        public static ResidueGraphOptions assembly(String assemblyIdentifier) {
            return new ResidueGraphOptions(ResidueGraphMode.ASSEMBLY, assemblyIdentifier);
        }

        /**
         * Report all contacts, regardless of assembly or applied transformation. This makes only sense when the graph
         * is computed on a substructure, defined by a number of {@link LabelSelection}.
         * @return the corresponding options
         */
        public static ResidueGraphOptions all() {
            return new ResidueGraphOptions(ResidueGraphMode.ALL, null);
        }
    }

    /**
     * Construct a residue graph.
     * @param structure the context
     * @param labelSelections residue keys (maybe subset, maybe all)
     * @param residues residue coordinates (maybe subset, maybe all)
     * @param strucmotifConfig global config
     */
    public ResidueGraph(Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues, StrucmotifConfig strucmotifConfig) {
        this.structure = structure;
        this.backboneDistances = new HashMap<>();
        this.sideChainDistances = new HashMap<>();
        this.angles = new HashMap<>();

        Map<IndexSelection, float[]> normalVectorMap = new LinkedHashMap<>();
        Map<IndexSelection, float[]> backboneVectors = new LinkedHashMap<>();
        Map<IndexSelection, float[]> sideChainVectors = new LinkedHashMap<>();
        List<IndexSelection> indexSelections = new ArrayList<>();
        for (int i = 0; i < labelSelections.size(); i++) {
            LabelSelection labelSelection = labelSelections.get(i);
            Map<LabelAtomId, float[]> residue = residues.get(i);
            int residueIndex = structure.getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId());
            IndexSelection indexSelection = new IndexSelection(labelSelection.getStructOperId(), residueIndex);
            ResidueType residueType = structure.getResidueType(residueIndex);

            float[] backbone = getBackboneCoords(residue);
            float[] sideChain;
            if (residueType == ResidueType.GLYCINE) {
                sideChain = getVirtualCB(residue);
            } else {
                sideChain = getSideChainCoords(residue);
            }

            if (backbone == null || sideChain == null) {
                continue;
            }

            indexSelections.add(indexSelection);
            backboneVectors.put(indexSelection, backbone);
            sideChainVectors.put(indexSelection, sideChain);
            normalVectorMap.put(indexSelection, normalVector(backbone, sideChain));
        }

        Map<String, String[]> assemblyMap = structure.getAssemblies();
        // handle case where undefined assemblies are allowed and no assembly info is present
        if (strucmotifConfig.isUndefinedAssemblies() && assemblyMap.isEmpty()) {
            assemblyMap.put(strucmotifConfig.getUndefinedAssemblyIdentifier(), structure.getLabelSelections()
                    .stream()
                    .map(LabelSelection::getLabelAsymId)
                    .distinct()
                    .map(c -> c + "_1")
                    .toArray(String[]::new));
        }

        this.selectionCount = labelSelections.size();
        this.numberOfResidues = backboneVectors.size();
        this.numberOfPairings = fillResidueGrid(backboneVectors, sideChainVectors, normalVectorMap, indexSelections, strucmotifConfig.getSquaredDistanceCutoff(), all(), assemblyMap);
    }

    /**
     * Construct a new residue graph from a full structure.
     * @param structure data
     * @param strucmotifConfig global config
     * @param options options to apply
     */
    public ResidueGraph(Structure structure, StrucmotifConfig strucmotifConfig, ResidueGraphOptions options) {
        this.structure = structure;
        this.backboneDistances = new HashMap<>();
        this.sideChainDistances = new HashMap<>();
        this.angles = new HashMap<>();

        // sort residues into chains
        // ${label_asym_id}: ${label_asym_id}-${label_seq_id}
        Map<String, List<LabelSelection>> chainMap = structure.getLabelSelections()
                .stream()
                .collect(Collectors.groupingBy(LabelSelection::getLabelAsymId));
        // ${assembly_id}: (${label_asym_id}_${struct_oper_id1}x${struct_oper_id2})[]
        Map<String, String[]> assemblyMap = structure.getAssemblies();

        // handle case where undefined assemblies are allowed and no assembly info is present
        if (strucmotifConfig.isUndefinedAssemblies() && assemblyMap.isEmpty()) {
            assemblyMap.put(strucmotifConfig.getUndefinedAssemblyIdentifier(), chainMap.keySet()
                    .stream()
                    .map(c -> c + "_" + Transformation.DEFAULT_OPERATOR)
                    .toArray(String[]::new));
        }

        List<float[]> originalBackboneVectors = new ArrayList<>();
        List<float[]> originalSideChainVectors = new ArrayList<>();
        for (int i = 0; i < structure.getResidueCount(); i++) {
            ResidueType residueType = structure.getResidueType(i);
            Map<LabelAtomId, float[]> residue = structure.manifestResidue(i);

            originalBackboneVectors.add(getBackboneCoords(residue));
            if (residueType == ResidueType.GLYCINE) {
                originalSideChainVectors.add(getVirtualCB(residue));
            } else {
                originalSideChainVectors.add(getSideChainCoords(residue));
            }
        }

        // all chains to generate
        List<String> assemblyInformation = assemblyMap.values()
                .stream()
                .flatMap(Arrays::stream)
                .distinct()
                .collect(Collectors.toList());

        // ${struct_oper_id}-${index}: float[]
        List<IndexSelection> residueKeys = new ArrayList<>();
        Map<IndexSelection, float[]> normalVectorMap = new LinkedHashMap<>();
        Map<IndexSelection, float[]> transformedBackboneVectors = new LinkedHashMap<>();
        Map<IndexSelection, float[]> transformedSideChainVectors = new LinkedHashMap<>();
        for (String a : assemblyInformation) {
            String[] split = a.split("_");
            String labelAsymId = split[0];
            String oper = split[1];
            // oper with ID '1' will be identity operation if nothing was defined in the source file
            Transformation transformation = structure.getTransformation(oper);

            // happens for non-polymer chains
            if (!chainMap.containsKey(labelAsymId)) continue;

            for (LabelSelection labelSelection : chainMap.get(labelAsymId)) {
                int residueIndex = structure.getResidueIndex(labelSelection.getLabelAsymId(), labelSelection.getLabelSeqId());

                float[] originalBackbone = originalBackboneVectors.get(residueIndex);
                float[] originalSideChain = originalSideChainVectors.get(residueIndex);
                if (originalBackbone == null || originalSideChain == null) {
                    continue;
                }

                IndexSelection key = new IndexSelection(oper, residueIndex);
                residueKeys.add(key);
                float[] backbone = new float[3];
                float[] sideChain = new float[3];
                transformation.transform(backbone, originalBackbone);
                transformedBackboneVectors.put(key, backbone);
                transformation.transform(sideChain, originalSideChain);
                transformedSideChainVectors.put(key, sideChain);

                normalVectorMap.put(key, normalVector(backbone, sideChain));
            }
        }

        this.selectionCount = transformedBackboneVectors.size();
        this.numberOfResidues = transformedBackboneVectors.size();
        this.numberOfPairings = fillResidueGrid(transformedBackboneVectors, transformedSideChainVectors, normalVectorMap, residueKeys, strucmotifConfig.getSquaredDistanceCutoff(), options, assemblyMap);
    }

    private int fillResidueGrid(Map<IndexSelection, float[]> backboneVectors, Map<IndexSelection, float[]> sideChainVectors, Map<IndexSelection, float[]> normalVectorMap, List<IndexSelection> indexSelections, float squaredCutoff, ResidueGraphOptions options, Map<String, String[]> assemblies) {
        // temporary ResidueGrid for efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(new ArrayList<>(backboneVectors.values()), squaredCutoff);
        Map<String, List<String>> assemblyMap = assemblies.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));

        ResidueGraphMode mode = options.mode;
        // track the first occurrence of each chain and consider that 'deposited'
        Set<String> acceptedChains = new HashSet<>();
        Set<String> acceptedOperators = new HashSet<>();
        if (mode == ResidueGraphMode.DEPOSITED || mode == ResidueGraphMode.DEPOSITED_AND_CONTACTS) {
            for (List<String> chainExprs : assemblyMap.values()) {
                for (String chainExpr : chainExprs) {
                    String chain = chainExpr.split("_")[0];
                    if (!acceptedChains.contains(chain)) {
                        acceptedChains.add(chain);
                        acceptedOperators.add(chainExpr);
                    }
                }
            }
        }

        // a specific assembly was requested
        String requestAssemblyIdentifier = options.assemblyIdentifier;
        List<String> requestChains = assemblyMap.get(requestAssemblyIdentifier);
        List<LabelSelection> labelSelections = structure.getLabelSelections();

        int size = 0;
        for (ResidueGrid.ResidueContact residueContact : residueGrid.getIndicesContacts()) {
            // avoid symmetry/duplicates
            if (residueContact.getI() >= residueContact.getJ()) {
                continue;
            }

            IndexSelection residueKey1 = indexSelections.get(residueContact.getI());
            String chainExpr1 = labelSelections.get(residueKey1.getIndex()).getLabelAsymId() + "_" + residueKey1.getStructOperId();

            IndexSelection residueKey2 = indexSelections.get(residueContact.getJ());
            String chainExpr2 = labelSelections.get(residueKey2.getIndex()).getLabelAsymId() + "_" + residueKey2.getStructOperId();

            switch (mode) {
                case DEPOSITED:
                    if (!acceptedOperators.contains(chainExpr1) || !acceptedOperators.contains(chainExpr2)) continue;
                    break;
                case DEPOSITED_AND_CONTACTS:
                    if (!acceptedOperators.contains(chainExpr1)) continue;
                    break;
                case ASSEMBLY:
                    if (!requestChains.contains(chainExpr1) || !requestChains.contains(chainExpr2)) continue;
                    break;
                case ALL:
                    // let everything else pass
            }

            // ensure that both chainExpressions occur in the same assembly
            if (assemblyMap.values().stream().noneMatch(opers -> opers.contains(chainExpr1) && opers.contains(chainExpr2))) {
                continue;
            }

            float[] normalVector1 = normalVectorMap.get(residueKey1);
            float[] normalVector2 = normalVectorMap.get(residueKey2);

            // ensure that side-chain atoms are available
            float[] sideChainCoordinates1 = sideChainVectors.get(residueKey1);
            float[] sideChainCoordinates2 = sideChainVectors.get(residueKey2);
            if (sideChainCoordinates1 == null || sideChainCoordinates2 == null) {
                continue;
            }

            Map<IndexSelection, Float> innerBackboneMap = backboneDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerBackboneMap.put(residueKey2, residueContact.getDistance());

            Map<IndexSelection, Float> innerSideChainMap = sideChainDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerSideChainMap.put(residueKey2, distance3d(sideChainCoordinates1, sideChainCoordinates2));

            Map<IndexSelection, Float> innerAngleMap = angles.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerAngleMap.put(residueKey2, angle(normalVector1, normalVector2));

            size++;
        }
        return size;
    }

    // already centered coordinates to save operations
    private static final List<float[]> REFERENCE_BACKBONE = List.of(new float[] { -0.698f, 0.184f, 1.008f }, // N
            new float[] { 0.525f, 0.109f, 0.200f }, // CA
            new float[] { 0.174f, -0.292f, -1.208f }); // C
    private static final float[] REFERENCE_CB = new float[] { 1.472f, -0.929f, 0.804f };
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

        Transformation transformation = QuaternionAlignmentService.align(coords, v, REFERENCE_BACKBONE, REFERENCE_CENTROID).getFirst();
        Algebra.multiply4d(v, transformation.getTransformationMatrix(), REFERENCE_CB);
        return v;
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

    /**
     * Compute the angle between 2 vectors. Must be normal vectors (will not be normalized in this call).
     * @param v1 first
     * @param v2 second
     * @return the angle in degrees
     */
    static float angle(float[] v1, float[] v2) {
        // be careful, normally dot product has to be divided by norm of v1 and v2
        float vDot = dotProduct3d(v1, v2);
        return (float) Math.toDegrees(Math.acos(capToInterval(-1, vDot, 1)));
    }

    /**
     * Reports the distance of backbone of 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the distance
     */
    public float getBackboneDistance(IndexSelection residue1, IndexSelection residue2) {
        return tryGet(backboneDistances, residue1, residue2);
    }

    /**
     * Reports the distance of side-chains of 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the distance
     */
    public float getSideChainDistance(IndexSelection residue1, IndexSelection residue2) {
        return tryGet(sideChainDistances, residue1, residue2);
    }

    /**
     * Reports the angle between 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the angle
     */
    public float getAngle(IndexSelection residue1, IndexSelection residue2) {
        return tryGet(angles, residue1, residue2);
    }

    private float tryGet(Map<IndexSelection, Map<IndexSelection, Float>> map, IndexSelection i1, IndexSelection i2) {
        if (map.containsKey(i1)) {
            Map<IndexSelection, Float> m = map.get(i1);
            if (m.containsKey(i2)) return m.get(i2);
        }
        if (map.containsKey(i2)) {
            Map<IndexSelection, Float> m = map.get(i2);
            if (m.containsKey(i1)) return m.get(i1);
        }
        return Float.MAX_VALUE;
    }

    /**
     * Reports the number of pairings present in this graph.
     * @return the number of registered pairs
     */
    public int getNumberOfPairings() {
        return numberOfPairings;
    }

    /**
     * Allows to sequentially traverse all pairings.
     * @return a stream of residue pairs
     */
    public Stream<Pair<IndexSelection, IndexSelection>> pairingsSequential() {
        // parallel streaming here will cause shuffling of motif descriptors and lead to alignment errors because correspondence cannot be asserted
        return backboneDistances.keySet()
                .stream()
                .flatMap(e -> pairs(e, false));
    }

    /**
     * Allows to traverse all pairings.
     * @return a stream of residue pairs
     */
    public Stream<Pair<IndexSelection, IndexSelection>> pairingsParallel() {
        return backboneDistances.keySet()
                .parallelStream()
                .flatMap(e -> pairs(e, true));
    }

    /**
     * Allows to traverse all residue pair occurrences. Will employ parallel stream.
     * @return a stream of {@link ResiduePairOccurrence} instances
     */
    public Stream<ResiduePairOccurrence> residuePairOccurrencesParallel() {
        return pairingsParallel()
                .map(this::createMotifOccurrence);
    }

    /**
     * Allows to traverse all residue pair occurrences. Will employ sequential stream.
     * @return a stream of {@link ResiduePairOccurrence} instances
     */
    public Stream<ResiduePairOccurrence> residuePairOccurrencesSequential() {
        return pairingsSequential()
                .map(this::createMotifOccurrence);
    }

    private ResiduePairOccurrence createMotifOccurrence(Pair<IndexSelection, IndexSelection> pair) {
        IndexSelection indexSelection1 = pair.getFirst();
        IndexSelection indexSelection2 = pair.getSecond();

        ResidueType residueType1 = structure.getResidueType(indexSelection1.getIndex());
        ResidueType residueType2 = structure.getResidueType(indexSelection2.getIndex());

        // first residue must have lower one-letter code - if not: flip
        if (residueType1.getInternalCode().compareTo(residueType2.getInternalCode()) > 0) {
            return createMotifOccurrence(new Pair<>(indexSelection2, indexSelection1));
        }

        // determine values
        DistanceType backboneDistance = DistanceType.ofDistance(getBackboneDistance(indexSelection1, indexSelection2));
        DistanceType sideChainDistance = DistanceType.ofDistance(getSideChainDistance(indexSelection1, indexSelection2));
        AngleType angle = AngleType.ofAngle(getAngle(indexSelection1, indexSelection2));

        ResiduePairDescriptor residuePairDescriptor = new ResiduePairDescriptor(residueType1,
                residueType2,
                backboneDistance,
                sideChainDistance,
                angle);
        // LabelSelection is needed to be able to map position-specific exchanges accurately
        ResiduePairIdentifier residuePairIdentifier = new IndexSelectionResiduePairIdentifier(indexSelection1,
                indexSelection2);
        return new ResiduePairOccurrence(residuePairDescriptor, residuePairIdentifier);
    }

    private Stream<Pair<IndexSelection, IndexSelection>> pairs(IndexSelection residue1, boolean parallel) {
        // retrieve all neighbors for id
        Map<IndexSelection, Float> map = backboneDistances.get(residue1);
        // can be empty
        if (map == null) return Stream.empty();

        if (parallel) {
            return map.keySet()
                    .parallelStream()
                    .map(residue2 -> new Pair<>(residue1, residue2));
        } else {
            return map.keySet()
                    .stream()
                    .map(residue2 -> new Pair<>(residue1, residue2));
        }
    }

    /**
     * Number of residues referenced by this graph.
     * @return an int
     */
    public int getNumberOfResidues() {
        return numberOfResidues;
    }

    /**
     * Checks whether the graph described by these residues is connected. Not optimized and only intended to check
     * validity of query motifs.
     * @return false if there are residues that aren't reachable from another residue or if the graph has no nodes
     */
    public boolean isConnected() {
        long start = System.nanoTime();
        if (selectionCount == 0) {
            return false;
        }

        Set<IndexSelection> visited = new HashSet<>();
        Set<Pair<IndexSelection, IndexSelection>> edges = residuePairOccurrencesSequential()
                .map(ResiduePairOccurrence::getResidueIdentifier)
                .map(p -> new Pair<>(p.getIndexSelection1(), p.getIndexSelection2()))
                .collect(Collectors.toSet());
        IndexSelection random = backboneDistances.keySet().iterator().next();
        dfs(random, edges, visited);

        logger.debug("Checked for connectedness in {} ms", (System.nanoTime() - start) * 0.001 * 0.001);
        return visited.size() == selectionCount;
    }

    private void dfs(IndexSelection currentNode, Set<Pair<IndexSelection, IndexSelection>> edges, Set<IndexSelection> visited) {
        visited.add(currentNode);

        for (Pair<IndexSelection, IndexSelection> edge : edges) {
            IndexSelection node1 = edge.getFirst();
            IndexSelection node2 = edge.getSecond();
            if (!currentNode.equals(node1) && !currentNode.equals(node2)) {
                continue;
            }

            IndexSelection neighbor = (node1.equals(currentNode)) ? node2 : node1;
            if (!visited.contains(neighbor)) {
                dfs(neighbor, edges, visited);
            }
        }
    }
}
