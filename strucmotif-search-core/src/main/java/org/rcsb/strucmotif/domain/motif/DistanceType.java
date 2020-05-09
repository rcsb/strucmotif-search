package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.math.Algebra;

/**
 * Binned representation of the distance between residues.
 */
public enum DistanceType {
    D0,
    D1,
    D2,
    D3,
    D4,
    D5,
    D6,
    D7,
    D8,
    D9,
    D10,
    D11,
    D12,
    D13,
    D14,
    D15,
    D16,
    D17,
    D18,
    D19,
    D20,
    D21,
    D22,
    D23,
    D24,
    D25,
    D26,
    D27,
    D28,
    D29,
    D30,
    D31,
    D32,
    D33,
    D34,
    D35,
    D36,
    D37,
    D38,
    D39,
    D40;

    public static final int BIN_SIZE = 1;

    /**
     * Convert enum to distance value in Angstrom (basically the lower bound of the interval).
     * @return lower bound of distances landing in this bin
     */
    public int getIntRepresentation() {
        return ordinal() * BIN_SIZE;
    }

    /**
     * Convert distance in Angstrom to enum value. Will cap results into interval of possible values.
     * @param distance raw distance
     * @return the corresponding bin
     */
    public static DistanceType ofDistance(double distance) {
        int i = (int) Math.round(distance / BIN_SIZE);
        if (i < 0) {
            return DistanceType.D0;
        } else if (i >= DistanceType.values().length) {
            return DistanceType.D40;
        } else {
            return DistanceType.values()[i];
        }
    }

    /**
     * Convert the ordinal to enum value. Somewhat trivial. Will cap results into interval of possible values.
     * @param ordinal the ordinal to get
     * @return the corresponding bin
     */
    public static DistanceType ofIntRepresentation(int ordinal) {
        return DistanceType.values()[Algebra.capToInterval(0, ordinal, values().length)];
    }
}
