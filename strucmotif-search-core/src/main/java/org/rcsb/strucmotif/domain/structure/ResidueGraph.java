package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.LabelSelectionResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.*;

/**
 * Represents the pseudo-graph defined by all residues. Some tricks to efficiently report distances and angles between
 * residues.
 */
public class ResidueGraph {
    private final Structure structure;
    private final Map<LabelSelection, Map<LabelSelection, Float>> backboneDistances;
    private final Map<LabelSelection, Map<LabelSelection, Float>> sideChainDistances;
    private final Map<LabelSelection, Map<LabelSelection, Float>> angles;
    private final int numberOfPairings;

    /**
     * Construct a residue graph from a structure.
     * @param structure the structure to process
     * @param squaredCutoff maximum squared residue distance
     */
    public ResidueGraph(Structure structure, float squaredCutoff) {
        this(structure, squaredCutoff, false);
    }

    /**
     * Construct a new residue graph.
     * @param structure data
     * @param squaredCutoff maximum dot product between atoms to consider
     * @param allowTransformed set to true during QueryStructure evaluation
     */
    public ResidueGraph(Structure structure, float squaredCutoff, boolean allowTransformed) {
        this.structure = structure;
        this.backboneDistances = new HashMap<>();
        this.sideChainDistances = new HashMap<>();
        this.angles = new HashMap<>();

        // sort residues into chains
        // ${label_asym_id}: ${label_asym_id}-${label_seq_id}
        Map<String, List<LabelSelection>> chainMap = structure.getResidueIdentifiers()
                .stream()
                .collect(Collectors.groupingBy(LabelSelection::getLabelAsymId));
        // ${assembly_id}: (${label_asym_id}_${struct_oper_id1}x${struct_oper_id2})[]
        Map<String, List<String>> assemblyMap = structure.getAssemblies();

        List<float[]> originalBackboneVectors = new ArrayList<>();
        List<float[]> originalSideChainVectors = new ArrayList<>();
        for (int i = 0; i < structure.getResidueCount(); i++) {
            ResidueType residueType = structure.getResidueType(i);
            Map<String, float[]> residue = structure.manifestResidue(i);

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

        // ${label_asym_id}-${label_seq_id}: float[]
        List<LabelSelection> residueKeys = new ArrayList<>();
        Map<LabelSelection, float[]> normalVectorMap = new LinkedHashMap<>();
        Map<LabelSelection, float[]> transformedBackboneVectors = new LinkedHashMap<>();
        Map<LabelSelection, float[]> transformedSideChainVectors = new LinkedHashMap<>();
        for (String a : assemblyInformation) {
            String[] split = a.split("_");
            String labelAsymId = split[0];
            String oper = split[1];
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

                LabelSelection key = new LabelSelection(labelSelection.getLabelAsymId(), oper, labelSelection.getLabelSeqId());
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

        // temporary ResidueGrid to efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(new ArrayList<>(transformedBackboneVectors.values()), squaredCutoff);

        int size = 0;
        for (ResidueGrid.ResidueContact residueContact : residueGrid.getIndicesContacts()) {
            // avoid symmetry/duplicates
            if (residueContact.getI() >= residueContact.getJ()) {
                continue;
            }

            LabelSelection residueKey1 = residueKeys.get(residueContact.getI());
            // 'dominant' residue has to be original by contract
            if (!allowTransformed && !residueKey1.getStructOperId().equals("1")) {
                continue;
            }

            float distance = residueContact.getDistance();
            LabelSelection residueKey2 = residueKeys.get(residueContact.getJ());
            float[] normalVector1 = normalVectorMap.get(residueKey1);
            float[] normalVector2 = normalVectorMap.get(residueKey2);

            // ensure that side-chain atoms are available
            float[] sideChainCoordinates1 = transformedSideChainVectors.get(residueKey1);
            float[] sideChainCoordinates2 = transformedSideChainVectors.get(residueKey2);
            if (sideChainCoordinates1 == null || sideChainCoordinates2 == null) {
                continue;
            }

            Map<LabelSelection, Float> innerPolymerAnchorMap = backboneDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerPolymerAnchorMap.put(residueKey2, distance);

            Map<LabelSelection, Float> innerInteractionCenterMap = sideChainDistances.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerInteractionCenterMap.put(residueKey2, distance3d(sideChainCoordinates1, sideChainCoordinates2));

            Map<LabelSelection, Float> innerAngleMap = angles.computeIfAbsent(residueKey1, key -> new HashMap<>());
            innerAngleMap.put(residueKey2, angle(normalVector1, normalVector2));

            size++;
        }
        this.numberOfPairings = size;
    }


    // already centered coordinates to save operations
    private static final List<float[]> REFERENCE_BACKBONE = List.of(new float[] { -0.698f, 0.184f, 1.008f }, // N
            new float[] { 0.525f, 0.109f, 0.200f }, // CA
            new float[] { 0.174f, -0.292f, -1.208f }); // C
    private static final float[] REFERENCE_CB = new float[] { 1.472f, -0.929f, 0.804f };
    private static final float[] REFERENCE_CENTROID = new float[3];

    static float[] getVirtualCB(Map<String, float[]> residue) {
        float[] n = residue.get("N");
        float[] ca = residue.get("CA");
        float[] c = residue.get("C");
        if (n == null || ca == null || c == null) {
            return null;
        }

        List<float[]> coords = List.of(n, ca, c);
        float[] v = Algebra.centroid3d(coords);

        Transformation transformation = QuaternionAlignmentService.align(coords, v, REFERENCE_BACKBONE, REFERENCE_CENTROID).getFirst();
        Algebra.multiply4d(v, transformation.getTransformationMatrix(), REFERENCE_CB);
        return v;
    }

    private static float[] getBackboneCoords(Map<String, float[]> residue) {
        if (residue.containsKey("CA")) {
            return residue.get("CA");
        }
        if (residue.containsKey("C4'")) {
            return residue.get("C4'");
        }
        return null;
    }

    private static float[] getSideChainCoords(Map<String, float[]> residue) {
        if (residue.containsKey("CB")) {
            return residue.get("CB");
        }
        if (residue.containsKey("C1'")) {
            return residue.get("C1'");
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
    public float getBackboneDistance(LabelSelection residue1, LabelSelection residue2) {
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
    public float getSideChainDistance(LabelSelection residue1, LabelSelection residue2) {
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
    public float getAngle(LabelSelection residue1, LabelSelection residue2) {
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
    public Stream<Pair<LabelSelection, LabelSelection>> pairingsSequential() {
        // parallel streaming here will cause shuffling of motif descriptors and lead to alignment errors because correspondence cannot be asserted
        return backboneDistances.keySet()
                .stream()
                .flatMap(e -> pairs(e, false));
    }

    /**
     * Allows to traverse all pairings.
     * @return a stream of residue pairs
     */
    public Stream<Pair<LabelSelection, LabelSelection>> pairingsParallel() {
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

    private ResiduePairOccurrence createMotifOccurrence(Pair<LabelSelection, LabelSelection> pair) {
        LabelSelection labelSelection1 = pair.getFirst();
        int residueIndex1 = structure.getResidueIndex(labelSelection1.getLabelAsymId(), labelSelection1.getLabelSeqId());
        LabelSelection labelSelection2 = pair.getSecond();
        int residueIndex2 = structure.getResidueIndex(labelSelection2.getLabelAsymId(), labelSelection2.getLabelSeqId());

        ResidueType residueType1 = structure.getResidueType(residueIndex1);
        ResidueType residueType2 = structure.getResidueType(residueIndex2);

        // first residue must have lower one-letter code - if not: flip
        if (residueType1.getOneLetterCode().compareTo(residueType2.getOneLetterCode()) > 0) {
            return createMotifOccurrence(new Pair<>(labelSelection2, labelSelection1));
        }

        // determine values
        DistanceType backboneDistance = DistanceType.ofDistance(getBackboneDistance(labelSelection1, labelSelection2));
        DistanceType sideChainDistance = DistanceType.ofDistance(getSideChainDistance(labelSelection1, labelSelection2));
        AngleType angle = AngleType.ofAngle(getAngle(labelSelection1, labelSelection2));

        ResiduePairDescriptor residuePairDescriptor = new ResiduePairDescriptor(residueType1,
                residueType2,
                backboneDistance,
                sideChainDistance,
                angle);
        // LabelSelection is needed to be able to map position-specific exchanges accurately
        ResiduePairIdentifier residuePairIdentifier = new LabelSelectionResiduePairIdentifier(labelSelection1,
                labelSelection2);
        return new ResiduePairOccurrence(residuePairDescriptor, residuePairIdentifier);
    }

    private Stream<Pair<LabelSelection, LabelSelection>> pairs(LabelSelection residue1, boolean parallel) {
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
