package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.*;
import java.util.stream.IntStream;

/**
 * A residue pair occurrence is the combination of its properties ({@link ResiduePairDescriptor}) and both identifiers
 * (wrapped as {@link ResiduePairIdentifier}).
 */
public class ResiduePairOccurrence {
    private final long residuePairIdentifier;
    private final int residuePairDescriptor;

    public ResiduePairOccurrence(long residuePairIdentifier, int residuePairDescriptor) {
        this.residuePairIdentifier = residuePairIdentifier;
        this.residuePairDescriptor = residuePairDescriptor;
    }

    public ResiduePairOccurrence(int residueIndex1, int residueIndex2, ResidueType residueType1, ResidueType residueType2, DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        this(ResiduePairIdentifier.encodeIdentifier(residueIndex1, residueIndex2), ResiduePairDescriptor.encodeDescriptor(residueType1.ordinal(), residueType2.ordinal(), backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal()));
    }

    /**
     * What are the properties of this pair?
     * @return a pair descriptor
     */
    public int getResiduePairDescriptor() {
        return residuePairDescriptor;
    }

    public ResidueType getResidueType1() {
        return ResiduePairDescriptor.getResidueType1(residuePairDescriptor);
    }

    public ResidueType getResidueType2() {
        return ResiduePairDescriptor.getResidueType2(residuePairDescriptor);
    }

    public DistanceType getBackboneDistance() {
        return ResiduePairDescriptor.getBackboneDistance(residuePairDescriptor);
    }

    public DistanceType getSideChainDistance() {
        return ResiduePairDescriptor.getSideChainDistance(residuePairDescriptor);
    }

    public AngleType getAngle() {
        return ResiduePairDescriptor.getAngle(residuePairDescriptor);
    }

    public long getResiduePairIdentifier() {
        return residuePairIdentifier;
    }

    /**
     * What's the first residue of this pair?
     * @return a residue index
     */
    public int getResidueIndex1() {
        return ResiduePairIdentifier.getResidueIndex1(residuePairIdentifier);
    }

    /**
     * What's the second residue of this pair?
     * @return a residue index
     */
    public int getResidueIndex2() {
        return ResiduePairIdentifier.getResidueIndex2(residuePairIdentifier);
    }

    /**
     * Access to both residue indices.
     * @return an IntStream
     */
    public IntStream residueIndices() {
        return IntStream.of(getResidueIndex1(), getResidueIndex2());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairOccurrence that = (ResiduePairOccurrence) o;
        return residuePairDescriptor == that.residuePairDescriptor && residuePairIdentifier == that.residuePairIdentifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(residuePairDescriptor, residuePairIdentifier);
    }

    @Override
    public String toString() {
        return "" + getResidueType1().getInternalCode() + getResidueType2().getInternalCode() + "-" +
                getBackboneDistance().ordinal() + "-" + getSideChainDistance().ordinal() + "-" + getAngle().ordinal() + " -> " +
                getResidueIndex1() + "-" + getResidueIndex2();
    }


    /**
     * Traverse all {@link ResiduePairDescriptor} instances which are compatible to this one given the specified
     * parameters and registered position-specific exchanges (if any).
     * @param backboneTolerance tolerance value
     * @param sideChainTolerance tolerance value
     * @param angleTolerance tolerance value
     * @param exchanges map of position-specific exchanges - may be empty
     * @return stream of all descriptors formed, encoded as int
     */
    public IntStream residuePairDescriptorsByTolerance(int backboneTolerance, int sideChainTolerance, int angleTolerance,
                                                       Map<Integer, Set<ResidueType>> exchanges) {
        int residueIndex2 = (int) residuePairIdentifier & 0xFF;
        int residueIndex1 = (int) (residuePairIdentifier >>> 32) & 0xFF;

        // we assign current component type for components without exchanges
        Set<ResidueType> residueTypes1 = exchanges.getOrDefault(residueIndex1, Set.of(getResidueType1()));
        Set<ResidueType> residueTypes2 = exchanges.getOrDefault(residueIndex2, Set.of(getResidueType2()));

        int backboneDistance = getBackboneDistance().ordinal();
        int sideChainDistance = getSideChainDistance().ordinal();
        int angle = getAngle().ordinal();
        Set<Integer> combinations = new HashSet<>();

        for (int i = -backboneTolerance; i <= backboneTolerance; i++) {
            int ii = backboneDistance + i;
            if (ii < 0 || ii >= DistanceType.values().length) {
                continue;
            }

            for (int j = -sideChainTolerance; j <= sideChainTolerance; j++) {
                int ij = sideChainDistance + j;
                if (ij < 0 || ij >= DistanceType.values().length) {
                    continue;
                }

                for (int k = -angleTolerance; k <= angleTolerance; k++) {
                    int ik = angle + k;
                    if (ik < 0 || ik >= AngleType.values().length) {
                        continue;
                    }

                    // happening for PSE
                    for (ResidueType residueType1 : residueTypes1) {
                        for (ResidueType residueType2 : residueTypes2) {
                            /*
                            ResiduePairDescriptors avoid combinations by enforcing alphabetic order, e.g. ED will be converted
                            to DE. This causes problems for PSE; the order may be changed implicitly causing several
                            issues downstream. A mutation of DE to DA will rearrange it to AD. Bad things will happen
                            when the path assembler checks for overlap with other motifs and assumes (respectively
                            simply cannot know) an unchanged order.

                            Long story short: That's why these cases are marked implicitly. The ResiduePairDescriptor is so
                            kind to flip and keep track of that.
                             */
                            boolean flipped = residueType1.getInternalCode().compareTo(residueType2.getInternalCode()) > 0;
                            int descriptor;
                            if (flipped) {
                                descriptor = ResiduePairDescriptor.encodeDescriptor(residueType2.ordinal(), residueType1.ordinal(), ii, ij, ik);
                                descriptor |= ResiduePairDescriptor.FLIPPED_MASK;
                            } else {
                                descriptor = ResiduePairDescriptor.encodeDescriptor(residueType1.ordinal(), residueType2.ordinal(), ii, ij, ik);
                            }
                            combinations.add(descriptor);
                        }
                    }
                }
            }
        }

        return combinations.parallelStream().mapToInt(i -> i);
    }
}
