package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.IndexResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.*;

/**
 * Represents the pseudo-graph defined by all residues. Some tricks to efficiently report distances and angles between
 * residues.
 */
public class ResidueGraph {
    private final Structure structure;
    private final Map<IndexSelection, Map<IndexSelection, Float>> backboneDistances;
    private final Map<IndexSelection, Map<IndexSelection, Float>> sideChainDistances;
    private final Map<IndexSelection, Map<IndexSelection, Float>> angles;
    private final int numberOfPairings;

    /**
     * Construct a residue graph.
     * @param structure the context
     * @param labelSelections residue keys (may be subset, may be all)
     * @param residues residue coordinates (may be subset, may be all)
     * @param squaredCutoff maximum distance of residue pairs to index
     * @param allowTransformed allow pairs between 2 transformed chains?
     */
    public ResidueGraph(Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues, float squaredCutoff, boolean allowTransformed) {
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

        this.numberOfPairings =  fillResidueGrid(backboneVectors, sideChainVectors, normalVectorMap, indexSelections, squaredCutoff, allowTransformed);
    }

    /**
     * Construct a new residue graph from a full structure.
     * @param structure data
     * @param squaredCutoff maximum dot product between atoms to consider
     * @param allowTransformed set to true during QueryStructure evaluation
     * @param allowUndefinedAssemblies set to true for computed structure models
     */
    public ResidueGraph(Structure structure, float squaredCutoff, boolean allowTransformed, boolean allowUndefinedAssemblies) {
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
        Map<String, Set<String>> assemblyMap = structure.getAssemblies();

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
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        // handle case where undefined assemblies are allowed and no assembly info is present
        if (allowUndefinedAssemblies && assemblyMap.isEmpty()) {
            assemblyInformation = chainMap.keySet()
                    .stream()
                    .map(labelAsymId -> labelAsymId + "_1")
                    .collect(Collectors.toList());
        }

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

        this.numberOfPairings = fillResidueGrid(transformedBackboneVectors, transformedSideChainVectors, normalVectorMap, residueKeys, squaredCutoff, allowTransformed);
    }

    private int fillResidueGrid(Map<IndexSelection, float[]> backboneVectors, Map<IndexSelection, float[]> sideChainVectors, Map<IndexSelection, float[]> normalVectorMap, List<IndexSelection> indexSelections, float squaredCutoff, boolean allowTransformed) {
        // temporary ResidueGrid to efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(new ArrayList<>(backboneVectors.values()), squaredCutoff);

        // if needed: check for first occurrence of chain (which may or may not be identity transform)
        List<LabelSelection> labelSelections = structure.getLabelSelections();
        Set<String> acceptedChains = new HashSet<>();
        Set<String> acceptedOperators = new HashSet<>();
        if (!allowTransformed) {
            for (Set<String> chainExprs : structure.getAssemblies().values()) {
                for (String chainExpr : chainExprs) {
                    String chain = chainExpr.split("_")[0];
                    if (!acceptedChains.contains(chain)) {
                        acceptedChains.add(chain);
                        acceptedOperators.add(chainExpr);
                    }
                }
            }
        }

        int size = 0;
        for (ResidueGrid.ResidueContact residueContact : residueGrid.getIndicesContacts()) {
            // avoid symmetry/duplicates
            if (residueContact.getI() >= residueContact.getJ()) {
                continue;
            }

            IndexSelection residueKey1 = indexSelections.get(residueContact.getI());
            String chainExpr1 = labelSelections.get(residueKey1.getIndex()).getLabelAsymId() + "_" + residueKey1.getStructOperId();
            // 'dominant' residue has to be original by contract
            if (!allowTransformed && !acceptedOperators.contains(chainExpr1)) {
                continue;
            }

            float distance = residueContact.getDistance();
            IndexSelection residueKey2 = indexSelections.get(residueContact.getJ());
            String chainExpr2 = labelSelections.get(residueKey2.getIndex()).getLabelAsymId() + "_" + residueKey2.getStructOperId();

            // ensure that both chainExpressions occur in the same assembly
            if (structure.getAssemblies().values().stream().noneMatch(opers -> opers.contains(chainExpr1) && opers.contains(chainExpr2))) {
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

            Map<IndexSelection, Float> innerPolymerAnchorMap = backboneDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerPolymerAnchorMap.put(residueKey2, distance);

            Map<IndexSelection, Float> innerInteractionCenterMap = sideChainDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerInteractionCenterMap.put(residueKey2, distance3d(sideChainCoordinates1, sideChainCoordinates2));

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
        try {
            return backboneDistances.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return backboneDistances.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Float.MAX_VALUE;
            }
        }
    }

    /**
     * Reports the distance of side-chains of 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the distance
     */
    public float getSideChainDistance(IndexSelection residue1, IndexSelection residue2) {
        try {
            return sideChainDistances.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return sideChainDistances.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Float.MAX_VALUE;
            }
        }
    }

    /**
     * Reports the angle between 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the angle
     */
    public float getAngle(IndexSelection residue1, IndexSelection residue2) {
        try {
            return angles.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return angles.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Float.MAX_VALUE;
            }
        }
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
        if (residueType1.getOneLetterCode().compareTo(residueType2.getOneLetterCode()) > 0) {
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
        ResiduePairIdentifier residuePairIdentifier = new IndexResiduePairIdentifier(indexSelection1,
                indexSelection2);
        return new ResiduePairOccurrence(residuePairDescriptor, residuePairIdentifier);
    }

    private Stream<Pair<IndexSelection, IndexSelection>> pairs(IndexSelection residue1, boolean parallel) {
        try {
            // retrieve all neighbors for id
            if (parallel) {
                return backboneDistances.get(residue1)
                        .keySet()
                        .parallelStream()
                        .map(residue2 -> new Pair<>(residue1, residue2));
            } else {
                return backboneDistances.get(residue1)
                        .keySet()
                        .stream()
                        .map(residue2 -> new Pair<>(residue1, residue2));
            }
        } catch (NullPointerException e) {
            // value can be empty
            return Stream.empty();
        }
    }
}
