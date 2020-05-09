package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.math.Algebra;

/**
 * Binned representation of the angle between residues.
 */
public enum AngleType {
    A0,
    A20,
    A40,
    A60,
    A80,
    A100,
    A120,
    A140,
    A160,
    A180;

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
