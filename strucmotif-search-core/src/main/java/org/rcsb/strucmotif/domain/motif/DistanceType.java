package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.math.Algebra;

/**
 * Binned representation of the distance between residues.
 */
public enum DistanceType {
    /**
     * [0, 0.5) A.
     */
    D0,
    /**
     * [0.5, 1.5) A.
     */
    D1,
    /**
     * [1.5, 2.5) A.
     */
    D2,
    /**
     * [2.5, 3.5) A.
     */
    D3,
    /**
     * [3.5, 4.5) A.
     */
    D4,
    /**
     * [4.5, 5.5) A.
     */
    D5,
    /**
     * [5.5, 6.5) A.
     */
    D6,
    /**
     * [6.5, 7.5) A.
     */
    D7,
    /**
     * [7.5, 8.5) A.
     */
    D8,
    /**
     * [8.5, 9.5) A.
     */
    D9,
    /**
     * [9.5, 10.5) A.
     */
    D10,
    /**
     * [10.5, 11.5) A.
     */
    D11,
    /**
     * [11.5, 12.5) A.
     */
    D12,
    /**
     * [12.5, 13.5) A.
     */
    D13,
    /**
     * [13.5, 14.5) A.
     */
    D14,
    /**
     * [14.5, 15.5) A.
     */
    D15,
    /**
     * [15.5, 16.5) A.
     */
    D16,
    /**
     * [16.5, 17.5) A.
     */
    D17,
    /**
     * [17.5, 18.5) A.
     */
    D18,
    /**
     * [18.5, 19.5) A.
     */
    D19,
    /**
     * [19.5, 20.5) A.
     */
    D20,
    /**
     * [20.5, 21.5) A.
     */
    D21,
    /**
     * [21.5, 22.5) A.
     */
    D22,
    /**
     * [22.5, 23.5) A.
     */
    D23,
    /**
     * [23.5, 24.5) A.
     */
    D24,
    /**
     * [24.5, 25.5) A.
     */
    D25,
    /**
     * [25.5, 26.5) A.
     */
    D26,
    /**
     * [26.5, 27.5) A.
     */
    D27,
    /**
     * [27.5, 28.5) A.
     */
    D28,
    /**
     * [28.5, 29.5) A.
     */
    D29,
    /**
     * [29.5, 30.5) A.
     */
    D30,
    /**
     * [30.5, 31.5) A.
     */
    D31; // this is the hard-limit for the distance between pairs

    /**
     * Width of a distance bin.
     */
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
    public static DistanceType ofDistance(float distance) {
        int i = Math.round(distance / BIN_SIZE);
        if (i < 0) {
            return DistanceType.D0;
        } else if (i >= DistanceType.values().length) {
            return DistanceType.D31;
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
