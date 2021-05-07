package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.math.Algebra;

/**
 * Binned representation of the angle between residues.
 */
public enum AngleType {
    /**
     * [0, 10) deg.
     */
    A0,
    /**
     * [10, 30) deg.
     */
    A20,
    /**
     * [30, 50) deg.
     */
    A40,
    /**
     * [50, 70) deg.
     */
    A60,
    /**
     * [70, 90) deg.
     */
    A80,
    /**
     * [90, 110) deg.
     */
    A100,
    /**
     * [110, 130) deg.
     */
    A120,
    /**
     * [130, 150) deg.
     */
    A140,
    /**
     * [150, 170) deg.
     */
    A160,
    /**
     * [170, 180]
     */
    A180;

    /**
     * Width of an angle bin.
     */
    public static final int BIN_SIZE = 20;

    /**
     * Convert enum to angle value in degree (basically the lower bound of the interval).
     * @return lower bound of angles landing in this bin
     */
    public int getIntRepresentation() {
        return ordinal() * BIN_SIZE;
    }

    /**
     * Convert angle in degrees to enum value. Will cap results into interval of possible values.
     * @param angle raw value
     * @return the corresponding bin
     */
    public static AngleType ofAngle(double angle) {
        int i = (int) Math.round(angle / BIN_SIZE);
        if (i < 0) {
            return AngleType.A0;
        } else if (i >= AngleType.values().length) {
            return AngleType.A180;
        } else {
            return AngleType.values()[i];
        }
    }

    /**
     * Convert the ordinal to enum value. Somewhat trivial. Will cap results into interval of possible values.
     * @param ordinal the ordinal to get
     * @return the corresponding bin
     */
    public static AngleType ofIntRepresentation(int ordinal) {
        return AngleType.values()[Algebra.capToInterval(0, ordinal, values().length)];
    }
}
