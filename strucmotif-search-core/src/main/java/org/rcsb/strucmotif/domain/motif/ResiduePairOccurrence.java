package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A residue pair occurrence is the combination of its properties ({@link ResiduePairDescriptor}) and both identifiers
 * (wrapped as {@link ResiduePairIdentifier}).
 */
public class ResiduePairOccurrence {
    private final ResiduePairDescriptor residuePairDescriptor;
    private final ResiduePairIdentifier residuePairIdentifier;

    public ResiduePairOccurrence(ResiduePairDescriptor residuePairDescriptor, ResiduePairIdentifier residuePairIdentifier) {
        this.residuePairDescriptor = residuePairDescriptor;
        this.residuePairIdentifier = residuePairIdentifier;
    }

    public ResiduePairDescriptor getResiduePairDescriptor() {
        return residuePairDescriptor;
    }

    public ResiduePairIdentifier getResidueIdentifier() {
        return residuePairIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairOccurrence that = (ResiduePairOccurrence) o;
        return Objects.equals(residuePairDescriptor, that.residuePairDescriptor) &&
                Objects.equals(residuePairIdentifier, that.residuePairIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(residuePairDescriptor, residuePairIdentifier);
    }

    @Override
    public String toString() {
        return residuePairDescriptor + " -> " + residuePairIdentifier;
    }

    /**
     * Traverse all {@link ResiduePairDescriptor} instances which are compatible to this one given the specified
     * parameters and registered position-specific exchanges (if any).
     * @param parameters query parameters
     * @param exchanges map of position-specific exchanges - may be empty
     * @return stream of all {@link ResiduePairDescriptor} instances formed
     */
    public Stream<ResiduePairDescriptor> residuePairDescriptorsByTolerance(Parameters parameters, Map<LabelSelection, Set<ResidueType>> exchanges) {
        // we assign current component type for components without exchanges
        Set<ResidueType> residueTypes1 = exchanges.getOrDefault(residuePairIdentifier.getLabelSelection1(), Set.of(residuePairDescriptor.getResidueType1()));
        Set<ResidueType> residueTypes2 = exchanges.getOrDefault(residuePairIdentifier.getLabelSelection2(), Set.of(residuePairDescriptor.getResidueType2()));

        int backboneDistanceTolerance = parameters.getBackboneDistanceTolerance();
        int sideChainDistanceTolerance = parameters.getSideChainDistanceTolerance();
        int angleTolerance = parameters.getAngleTolerance();

        int backboneDistance = residuePairDescriptor.getBackboneDistance().ordinal();
        int sideChainDistance = residuePairDescriptor.getSideChainDistance().ordinal();
        int angle = residuePairDescriptor.getAngle().ordinal();
        List<ResiduePairDescriptor> combinations = new ArrayList<>();

        for (int i = -backboneDistanceTolerance; i <= backboneDistanceTolerance; i++) {
            int ii = backboneDistance + i;
            if (ii < 0 || ii >= DistanceType.values().length) {
                continue;
            }

            for (int j = -sideChainDistanceTolerance; j <= sideChainDistanceTolerance; j++) {
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
                            ResiduePairDescriptor derivedResiduePairDescriptor = new ResiduePairDescriptor(residueType1,
                                    residueType2,
                                    DistanceType.values()[ii],
                                    DistanceType.values()[ij],
                                    AngleType.values()[ik],
                                    // track how different any tolerate deviation of this descriptor is from the original definition
                                    residuePairDescriptor);
                            combinations.add(derivedResiduePairDescriptor);
                        }
                    }
                }
            }
        }

        return combinations.parallelStream();
    }
}
