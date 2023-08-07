package org.rcsb.strucmotif.domain.motif;

/**
 * A residue pair descriptor is a long that combines 2 residue indices. The upper 32 bit (A) are the 1st value, the
 * lower 32 bit (B) contain the 2nd value.
 *
 * <pre>AAAAAAAA AAAAAAAA AAAAAAAA AAAAAAAA BBBBBBBB BBBBBBBB BBBBBBBB BBBBBBBB</pre>
 */
public class ResiduePairIdentifier {
    private ResiduePairIdentifier() {
        // deny
    }

    /**
     * Encode (residueIndex1, residueIndex2) as long.
     * @param residueIndex1 1st index
     * @param residueIndex2 2nd index
     * @return both represented as a single long
     */
    public static long encodeIdentifier(int residueIndex1, int residueIndex2) {
        return (long) residueIndex1 << 32 | residueIndex2;
    }

    /**
     * Extract the 1st index from a long identifier.
     * @param residuePairIdentifier long
     * @return 1st index as int
     */
    public static int getResidueIndex1(long residuePairIdentifier) {
        return (int) (residuePairIdentifier >>> 32);
    }

    /**
     * Extract the 2nd index from a long identifier.
     * @param residuePairIdentifier long
     * @return 2nd index as int
     */
    public static int getResidueIndex2(long residuePairIdentifier) {
        return (int) residuePairIdentifier;
    }

    /**
     * Pretty-print a long identifier.
     * @param residuePairIdentifier long
     * @return human-readable String
     */
    public static String toString(long residuePairIdentifier) {
        return getResidueIndex1(residuePairIdentifier) + "-" + getResidueIndex2(residuePairIdentifier);
    }
}
