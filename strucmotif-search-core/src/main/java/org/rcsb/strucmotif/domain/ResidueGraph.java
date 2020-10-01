package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.*;

/**
 * Represents the pseudo-graph defined by all residues. Some tricks to efficiently report distances and angles between
 * residues.
 */
public class ResidueGraph {
    private final Map<Residue, Map<Residue, Double>> backboneDistances;
    private final Map<Residue, Map<Residue, Double>> sideChainDistances;
    private final Map<Residue, Map<Residue, Double>> angles;
    private final int numberOfPairings;
    private final Map<Residue, IndexSelection> indexSelectionResolver;
    private final Map<Residue, LabelSelection> labelSelectionResolver;

    public ResidueGraph(Structure structure, double squaredCutoff) {
        this.indexSelectionResolver = new HashMap<>();
        this.labelSelectionResolver = new HashMap<>();

        this.backboneDistances = new LinkedHashMap<>();
        this.sideChainDistances = new LinkedHashMap<>();
        this.angles = new LinkedHashMap<>();
        Map<Residue, double[]> normalVectorLookup = new HashMap<>();

        int size = 0;
        for (int chainId1 = 0; chainId1 < structure.getChains().size(); chainId1++) {
            Chain chain1 = structure.getChains().get(chainId1);
            String assemblyId1 = chain1.getChainIdentifier().getStructOperId();
            String labelAsymId1 = chain1.getChainIdentifier().getLabelAsymId();

            // dominant chain has to be original
            if (!chain1.isNeutral()) {
                continue;
            }

            for (int residueId1 = 0; residueId1 < chain1.getResidues().size(); residueId1++) {
                Residue residue1 = chain1.getResidues().get(residueId1);

                try {
                    double[] backboneCoordinates1 = residue1.getBackboneCoordinates();
                    double[] sideChainCoordinates1 = residue1.getSideChainCoordinates();

                    // nothing to do if either representative is missing - this also implicitly omits stuff which is neither amino acid nor nucleotide
                    if (backboneCoordinates1 == null || sideChainCoordinates1 == null) {
                        continue;
                    }

                    double[] normalVector1 = normalVectorLookup.computeIfAbsent(residue1, e -> normalVector(backboneCoordinates1, sideChainCoordinates1));
                    indexSelectionResolver.put(residue1, new IndexSelection(assemblyId1, residue1.getResidueIdentifier().getIndex()));
                    labelSelectionResolver.put(residue1, new LabelSelection(labelAsymId1, assemblyId1, residue1.getResidueIdentifier().getLabelSeqId()));

                    for (int chainId2 = chainId1; chainId2 < structure.getChains().size(); chainId2++) {
                        Chain chain2 = structure.getChains().get(chainId2);
                        String assemblyId2 = chain2.getChainIdentifier().getStructOperId();
                        String labelAsymId2 = chain2.getChainIdentifier().getLabelAsymId();

                        for (int residueId2 = 0; residueId2 < chain2.getResidues().size(); residueId2++) {
                            if (chain1.equals(chain2) && residueId2 <= residueId1) {
                                continue;
                            }

                            Residue residue2 = chain2.getResidues().get(residueId2);

                            try {
                                double[] backboneCoordinates2 = residue2.getBackboneCoordinates();
                                double[] sideChainCoordinates2 = residue2.getSideChainCoordinates();

                                if (backboneCoordinates2 == null || sideChainCoordinates2 == null) {
                                    continue;
                                }

                                // additional safety net for duplicated keys and distance 0
                                /*
                                 get ready to get (judge)mental: we consider components equal if their identifier matches -
                                 however for this scenario we want components in assemblies to pass - therefore, we additionally
                                 check for agreement at atom level
                                 */
                                if (residue1.equals(residue2)) {
                                    continue;
                                }

                                double squaredDistance = distanceSquared3d(backboneCoordinates1, backboneCoordinates2);
                                if (squaredDistance < squaredCutoff) {
                                    Map<Residue, Double> innerPolymerAnchorMap = backboneDistances.computeIfAbsent(residue1, key -> new HashMap<>());
                                    innerPolymerAnchorMap.put(residue2, Math.sqrt(squaredDistance));

                                    Map<Residue, Double> innerInteractionCenterMap = sideChainDistances.computeIfAbsent(residue1, key -> new HashMap<>());
                                    innerInteractionCenterMap.put(residue2, distance3d(sideChainCoordinates1, sideChainCoordinates2));

                                    // compute angle between amino acid planes
                                    double[] normalVector2 = normalVectorLookup.computeIfAbsent(residue2, e -> normalVector(backboneCoordinates2, sideChainCoordinates2));
                                    indexSelectionResolver.put(residue2, new IndexSelection(assemblyId2, residue2.getResidueIdentifier().getIndex()));
                                    labelSelectionResolver.put(residue2, new LabelSelection(labelAsymId2, assemblyId2, residue2.getResidueIdentifier().getLabelSeqId()));

                                    Map<Residue, Double> innerAngleMap = angles.computeIfAbsent(residue1, key -> new HashMap<>());
                                    innerAngleMap.put(residue2, angle(normalVector1, normalVector2));

                                    size++;
                                }
                            } catch (Exception e) {
                                // happens when atoms are missing
                            }
                        }
                    }
                } catch (Exception e) {
                    // happens when atoms are missing
                }
            }
        }
        this.numberOfPairings = size;
    }

    private static double[] normalVector(double[] a, double[] b) {
        double[] ba = subtract3d(b, a);
        return normalize3d(ba);
    }

    /**
     * Compute the angle between 2 vectors. Must be normal vectors (will not be normalized in this call).
     * @param vector1 first
     * @param vector2 second
     * @return the angle in degrees
     */
    static double angle(double[] vector1, double[] vector2) {
        // be careful, normally dot product has to be divided by norm of v1 and v2
        double vDot = dotProduct3d(vector1, vector2);
        return Math.toDegrees(Math.acos(capToInterval(-1, vDot, 1)));
    }

    /**
     * Reports the distance of backbone of 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the distance
     */
    public double getBackboneDistance(Residue residue1, Residue residue2) {
        try {
            return backboneDistances.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return backboneDistances.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Double.MAX_VALUE;
            }
        }
    }

    /**
     * Reports the distance of side-chains of 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the distance
     */
    public double getSideChainDistance(Residue residue1, Residue residue2) {
        try {
            return sideChainDistances.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return sideChainDistances.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Double.MAX_VALUE;
            }
        }
    }

    /**
     * Reports the angle between 2 residues.
     * @param residue1 first
     * @param residue2 second
     * @return the angle
     */
    public double getAngle(Residue residue1, Residue residue2) {
        try {
            return angles.get(residue1).get(residue2);
        } catch (NullPointerException e1) {
            try {
                return angles.get(residue2).get(residue1);
            } catch (NullPointerException e2) {
                return Double.MAX_VALUE;
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
    public Stream<Pair<Residue, Residue>> pairingsSequential() {
        // parallel streaming here will cause shuffling of motif descriptors and lead to alignment errors because correspondence cannot be asserted
        return backboneDistances.keySet()
                .stream()
                .flatMap(e -> pairs(e, false));
    }

    /**
     * Allows to traverse all pairings.
     * @return a stream of residue pairs
     */
    public Stream<Pair<Residue, Residue>> pairingsParallel() {
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

    public Stream<ResiduePairOccurrence> residuePairOccurrencesSequential() {
        return pairingsSequential()
                .map(this::createMotifOccurrence);
    }

    private ResiduePairOccurrence createMotifOccurrence(Pair<Residue, Residue> pair) {
        Residue residue1 = pair.getFirst();
        Residue residue2 = pair.getSecond();

        ResidueType residueType1 = residue1.getResidueIdentifier().getResidueType();
        ResidueType residueType2 = residue2.getResidueIdentifier().getResidueType();

        // first residue has lower one-letter code - if not flip
        if (residueType1.getOneLetterCode().compareTo(residueType2.getOneLetterCode()) > 0) {
            return createMotifOccurrence(new Pair<>(residue2, residue1));
        }

        // determine values
        DistanceType backboneDistance = DistanceType.ofDistance(getBackboneDistance(residue1, residue2));
        DistanceType sideChainDistance = DistanceType.ofDistance(getSideChainDistance(residue1, residue2));
        AngleType angle = AngleType.ofAngle(getAngle(residue1, residue2));

        ResiduePairDescriptor residuePairDescriptor = new ResiduePairDescriptor(residueType1,
                residueType2,
                backboneDistance,
                sideChainDistance,
                angle,
                null);
        // IndexSelection is needed to build and match to lookup
        // LabelSelection is needed to be able to map position-specific exchanges accurately
        ResiduePairIdentifier residuePairIdentifier = new ResiduePairIdentifier(indexSelectionResolver.get(residue1),
                indexSelectionResolver.get(residue2),
                labelSelectionResolver.get(residue1),
                labelSelectionResolver.get(residue2));
        return new ResiduePairOccurrence(residuePairDescriptor, residuePairIdentifier);
    }

    private Stream<Pair<Residue, Residue>> pairs(Residue residue1, boolean parallel) {
        try {
            // retrieve all neighbors for id
            if (parallel) {
                return backboneDistances.get(residue1)
                        .keySet()
                        .parallelStream()
                        .map(entity2 -> new Pair<>(residue1, entity2));
            } else {
                return backboneDistances.get(residue1)
                        .keySet()
                        .stream()
                        .map(entity2 -> new Pair<>(residue1, entity2));
            }
        } catch (NullPointerException e) {
            // value can be empty
            return Stream.empty();
        }
    }
}
