package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Objects;

/**
 * Captures all properties of a residue pair (i.e. types, distances and angle).
 */
public class ResiduePairDescriptor {
    private final ResidueType residueType1;
    private final ResidueType residueType2;
    private final DistanceType backboneDistance;
    private final DistanceType sideChainDistance;
    private final AngleType angle;
    private final boolean flipped;

    /**
     * Construct a residue pair descriptor capturing all properties of this pair.
     * @param residueType1 the first type
     * @param residueType2 the second type
     * @param backboneDistance the backbone distance
     * @param sideChainDistance the side-chain distance
     * @param angle the angle
     */
    public ResiduePairDescriptor(ResidueType residueType1,
                                 ResidueType residueType2,
                                 DistanceType backboneDistance,
                                 DistanceType sideChainDistance,
                                 AngleType angle) {
        // determine if descriptor needs to be flip implicitly
        this.flipped = residueType1.getInternalCode().compareTo(residueType2.getInternalCode()) > 0;
        this.residueType1 = flipped ? residueType2 : residueType1;
        this.residueType2 = flipped ? residueType1 : residueType2;
        this.backboneDistance = backboneDistance;
        this.sideChainDistance = sideChainDistance;
        this.angle = angle;
    }

    /**
     * Residue type (label_comp_id) of first element.
     * @return a residue type
     */
    public ResidueType getResidueType1() {
        return residueType1;
    }

    /**
     * Residue type (label_comp_id) of second element.
     * @return a residue type
     */
    public ResidueType getResidueType2() {
        return residueType2;
    }

    /**
     * Binned backbone distance for this pair.
     * @return a distance type
     */
    public DistanceType getBackboneDistance() {
        return backboneDistance;
    }

    /**
     * Binned side-chain distance for this pair.
     * @return a distance type
     */
    public DistanceType getSideChainDistance() {
        return sideChainDistance;
    }

    /**
     * Binned angle for this pair.
     * @return an angle type
     */
    public AngleType getAngle() {
        return angle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResiduePairDescriptor that = (ResiduePairDescriptor) o;
        return flipped == that.flipped &&
                residueType1 == that.residueType1 &&
                residueType2 == that.residueType2 &&
                backboneDistance == that.backboneDistance &&
                sideChainDistance == that.sideChainDistance &&
                angle == that.angle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(residueType1, residueType2, backboneDistance, sideChainDistance, angle, flipped);
    }

    @Override
    public String toString() {
        return residueType1.getInternalCode() +
                residueType2.getInternalCode() + "-" +
                backboneDistance.getIntRepresentation() + "-" +
                sideChainDistance.getIntRepresentation() + "-" +
                angle.ordinal();
    }

    /**
     * We have to keep track whether this descriptor should match an original one (i.e. during a position-specific
     * exchange) but was flipped in order to library contract (e.g. GH - HIS is replaced by ALA - will become AG).
     * @return true if flipped and special care must be taken during overlap computation (otherwise all overlap values
     * would be mismatching)
     */
    public boolean isFlipped() {
        return flipped;
    }

    /**
     * Extra work is needed when a descriptor describes the same residue type in both positions.
     * @return true if this descriptor is ambiguous because both residue types are the same
     */
    public boolean isAmbiguous() {
        return residueType1 == residueType2;
    }
}
