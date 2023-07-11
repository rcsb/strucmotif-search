package org.rcsb.strucmotif.domain.motif;

/**
 * A residue pair descriptor is a long that combines 2 residue indices. The upper 32 bit (A) are the 1st value, the
 * lower 32 bit (B) contain the 2nd value.
 *
 * <pre>AAAAAAAA AAAAAAAA AAAAAAAA AAAAAAAA BBBBBBBB BBBBBBBB BBBBBBBB BBBBBBBB</pre>
 */
public class ResiduePairIdentifier {
    public static long encodeIdentifier(int residueIndex1, int residueIndex2) {
        return (long) residueIndex1 << 32 | residueIndex2;
    }

    public static int getResidueIndex1(long residuePairIdentifier) {
        return (int) (residuePairIdentifier >>> 32);
    }

    public static int getResidueIndex2(long residuePairIdentifier) {
        return (int) residuePairIdentifier;
    }

    public static String toString(long residuePairIdentifier) {
        return getResidueIndex1(residuePairIdentifier) + "-" + getResidueIndex2(residuePairIdentifier);
    }
}
