package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairOccurrence;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
    private final Map<Residue, LabelSelection> labelSelectionResolver;

    /**
     * Construct a residue graph from a structure.
     * @param structure the structure to process
     * @param squaredCutoff maximum squared residue distance
     */
    public ResidueGraph(Structure structure, double squaredCutoff) {
        this(structure, squaredCutoff, false);
    }

    /**
     * Construct a new residue graph.
     * @param structure data
     * @param squaredCutoff maximum dot product between atoms to consider
     * @param allowTransformed set to true during QueryStructure evaluation
     */
    public ResidueGraph(Structure structure, double squaredCutoff, boolean allowTransformed) {
        this.labelSelectionResolver = new HashMap<>();

        this.backboneDistances = new LinkedHashMap<>();
        this.sideChainDistances = new LinkedHashMap<>();
        this.angles = new LinkedHashMap<>();

        // temporary collection of residues in 'original' chains (i.e. not transformed as the result of a bioassembly)
        Set<Residue> originalResidues = new HashSet<>();
        Map<Residue, double[]> normalVectorLookup = new HashMap<>();
        for (Chain chain : structure.getChains()) {
            ChainIdentifier chainIdentifier = chain.getChainIdentifier();
            String labelAsymId = chainIdentifier.getLabelAsymId();
            String structOperId = chainIdentifier.getStructOperId();
            for (Residue residue : chain.getResidues()) {
                if (!chain.isTransformed()) {
                    originalResidues.add(residue);
                }

                double[] backboneCoordinates = residue.getBackboneCoordinates();
                double[] sideChainCoordinates = residue.getSideChainCoordinates();
                // nothing to do if either representative is missing - this also implicitly omits stuff which is neither amino acid nor nucleotide
                if (backboneCoordinates == null || sideChainCoordinates == null) {
                    continue;
                }

                normalVectorLookup.put(residue, normalVector(backboneCoordinates, sideChainCoordinates));
                labelSelectionResolver.put(residue, new LabelSelection(labelAsymId, structOperId, residue.getResidueIdentifier().getLabelSeqId()));
            }
        }

        // temporary ResidueGrid to efficient distance calculation
        ResidueGrid residueGrid = new ResidueGrid(structure, squaredCutoff);

        int size = 0;
        for (ResidueGrid.ResidueContact residueContact : residueGrid.getIndicesContacts()) {
            // avoid symmetry/duplicates
            if (residueContact.getI() >= residueContact.getJ()) {
                continue;
            }

            Residue residue1 = residueGrid.getResidue(residueContact.getI());
            // 'dominant' residue has to be original by contract
            if (!allowTransformed && !originalResidues.contains(residue1)) {
                continue;
            }

            double distance = residueContact.getDistance();
            Residue residue2 = residueGrid.getResidue(residueContact.getJ());
            double[] normalVector1 = normalVectorLookup.get(residue1);
            double[] normalVector2 = normalVectorLookup.get(residue2);

            // ensure that side-chain atoms are available
            double[] sideChainCoordinates1 = residue1.getSideChainCoordinates();
            double[] sideChainCoordinates2 = residue2.getSideChainCoordinates();
            if (sideChainCoordinates1 == null || sideChainCoordinates2 == null) {
                continue;
            }

            Map<Residue, Double> innerPolymerAnchorMap = backboneDistances.computeIfAbsent(residue1, key -> new HashMap<>());
            innerPolymerAnchorMap.put(residue2, distance);

            Map<Residue, Double> innerInteractionCenterMap = sideChainDistances.computeIfAbsent(residue1, key -> new HashMap<>());
            innerInteractionCenterMap.put(residue2, distance3d(sideChainCoordinates1, sideChainCoordinates2));

            Map<Residue, Double> innerAngleMap = angles.computeIfAbsent(residue1, key -> new HashMap<>());
            innerAngleMap.put(residue2, angle(normalVector1, normalVector2));

            size++;
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

    /**
     * Allows to traverse all residue pair occurrences. Will employ sequential stream.
     * @return a stream of {@link ResiduePairOccurrence} instances
     */
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
        ResiduePairIdentifier residuePairIdentifier = new ResiduePairIdentifier(labelSelectionResolver.get(residue1),
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
