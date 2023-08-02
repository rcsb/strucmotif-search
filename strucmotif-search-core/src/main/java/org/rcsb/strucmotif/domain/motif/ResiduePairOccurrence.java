package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A residue pair occurrence is the combination of its properties ({@link ResiduePairDescriptor}) and both identifiers
 * (wrapped as {@link ResiduePairIdentifier}).
 */
public class ResiduePairOccurrence {
    private final long residuePairIdentifier;
    private final int residuePairDescriptor;

    /**
     * Construct an occurrence, a pair of identifier and its descriptor.
     * @param residuePairIdentifier both residue indices encoded as long
     * @param residuePairDescriptor all properties encoded as int
     */
    public ResiduePairOccurrence(long residuePairIdentifier, int residuePairDescriptor) {
        this.residuePairIdentifier = residuePairIdentifier;
        this.residuePairDescriptor = residuePairDescriptor;
    }

    /**
     * Construct an occurrence, a pair of identifier and its descriptor.
     * @param residueIndex1 1st index
     * @param residueIndex2 2nd index
     * @param residueType1 1st type
     * @param residueType2 2nd type
     * @param backboneDistance backbone distance type
     * @param sideChainDistance side-chain distance type
     * @param angle angle type
     */
    public ResiduePairOccurrence(int residueIndex1, int residueIndex2, ResidueType residueType1, ResidueType residueType2, DistanceType backboneDistance, DistanceType sideChainDistance, AngleType angle) {
        this(ResiduePairIdentifier.encodeIdentifier(residueIndex1, residueIndex2), ResiduePairDescriptor.encodeDescriptor(residueType1.ordinal(), residueType2.ordinal(), backboneDistance.ordinal(), sideChainDistance.ordinal(), angle.ordinal()));
    }

    /**
     * Sort a list of occurrences and move the ones that are most restrictive and/or most cheap to evaluate to the
     * front.
     * @param residuePairOccurrences ordered list of occurrences
     * @return the same list, ordered
     */
    public static List<ResiduePairOccurrence> sort(List<ResiduePairOccurrence> residuePairOccurrences) {
        return residuePairOccurrences.stream()
                .sorted(INFORMATIVENESS_COMPARATOR)
                .collect(Collectors.toList());
    }

    /**
     * What are the properties of this pair?
     * @return a pair descriptor
     */
    public int getResiduePairDescriptor() {
        return residuePairDescriptor;
    }

    /**
     * Extract the 1st residue type.
     * @return residue type
     */
    public ResidueType getResidueType1() {
        return ResiduePairDescriptor.getResidueType1(residuePairDescriptor);
    }

    /**
     * Extract the 2nd residue type.
     * @return residue type
     */
    public ResidueType getResidueType2() {
        return ResiduePairDescriptor.getResidueType2(residuePairDescriptor);
    }

    /**
     * Extract the backbone distance type.
     * @return distance type
     */
    public DistanceType getBackboneDistance() {
        return ResiduePairDescriptor.getBackboneDistance(residuePairDescriptor);
    }

    /**
     * Extract the side-chain distance type.
     * @return distance type
     */
    public DistanceType getSideChainDistance() {
        return ResiduePairDescriptor.getSideChainDistance(residuePairDescriptor);
    }

    /**
     * Extract the angle type.
     * @return angle type
     */
    public AngleType getAngle() {
        return ResiduePairDescriptor.getAngle(residuePairDescriptor);
    }

    /**
     * Report the identifiers as encoded long.
     * @return long representing both residue indices
     */
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
        return ResiduePairDescriptor.toString(residuePairDescriptor) + " -> " + ResiduePairIdentifier.toString(residuePairIdentifier);
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
        int residueIndex1 = ResiduePairIdentifier.getResidueIndex1(residuePairIdentifier);
        int residueIndex2 = ResiduePairIdentifier.getResidueIndex2(residuePairIdentifier);

        // we assign current component type for components without exchanges
        Set<ResidueType> residueTypes1 = exchanges.getOrDefault(residueIndex1, Set.of(getResidueType1()));
        Set<ResidueType> residueTypes2 = exchanges.getOrDefault(residueIndex2, Set.of(getResidueType2()));

        int backboneDistance = getBackboneDistance().ordinal();
        int sideChainDistance = getSideChainDistance().ordinal();
        int angle = getAngle().ordinal();
        Set<Integer> combinations = new HashSet<>();

        for (int i = -backboneTolerance; i <= backboneTolerance; i++) {
            int ii = backboneDistance + i;
            if (ii < 0 || ii >= DistanceType.values.length) {
                continue;
            }

            for (int j = -sideChainTolerance; j <= sideChainTolerance; j++) {
                int ij = sideChainDistance + j;
                if (ij < 0 || ij >= DistanceType.values.length) {
                    continue;
                }

                for (int k = -angleTolerance; k <= angleTolerance; k++) {
                    int ik = angle + k;
                    if (ik < 0 || ik >= AngleType.values.length) {
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

    // unknown, rare or problematic -- to the front
    private static final int DEFAULT_INFORMATIVENESS = 0;
    // these are encoded residue type combinations, sorted ascending by their absolute frequency
    private static final int[] SORTED_RESIDUE_TYPE_COMBINATIONS = new int[] {
            1504, 1693, 1501, 1696, 1564, 1566, 1694, 1440, 1565, 1500, 1692, 2210, 1502, 1428, 1556, 1492, 1684, 1409,
            1473, 1665, 1889, 1537, 2081, 1825, 1953, 1418, 1546, 1674, 1482, 1670, 1478, 2132, 1414, 1542, 1476, 1493,
            97, 1668, 1540, 1412, 1685, 1557, 1486, 1429, 1550, 1410, 1422, 1474, 1666, 1538, 1483, 1678, 1419, 1547,
            1485, 1677, 1475, 1421, 1675, 417, 1549, 1411, 1539, 2122, 1667, 1479, 1491, 1671, 1543, 1683, 1427, 1489,
            1415, 1555, 1553, 1488, 1472, 1425, 1681, 2133, 1481, 1424, 2123, 1408, 1417, 1680, 1536, 1477, 1545, 1552,
            1673, 1413, 1664, 1669, 2126, 1541, 289, 1495, 1430, 1480, 1560, 2120, 2068, 2125, 1487, 161, 1857, 1672,
            1544, 1416, 2049, 1690, 2129, 2127, 1423, 1679, 2128, 1551, 481, 1876, 1793, 2131, 225, 1921, 353, 1812,
            1940, 33, 1432, 1431, 1498, 1562, 2121, 2058, 1496, 1434, 1866, 1802, 1930, 2052, 2050, 2069, 1860, 1858,
            2051, 1877, 1796, 1794, 2054, 1813, 2062, 1859, 1924, 1862, 1941, 1795, 2059, 1798, 1870, 1922, 1806, 1867,
            2055, 2061, 1863, 1923, 1803, 1926, 1934, 1799, 1869, 2065, 2064, 1805, 1931, 2057, 1872, 1927, 1873, 1933,
            1808, 2067, 1809, 1865, 32, 1937, 1801, 1875, 1811, 1936, 1856, 1929, 2053, 1792, 1939, 1861, 1920, 1797,
            2056, 2063, 1925, 1864, 2080, 1800, 1871, 1807, 1928, 1935, 1885, 1820, 1300, 1888, 1950, 1824, 1821, 1952,
            1886, 1822, 84, 65, 2145, 390, 404, 660, 650, 74, 70, 916, 724, 1301, 78, 394, 532, 852, 85, 75, 148, 910,
            276, 77, 980, 1365, 212, 1108, 66, 72, 398, 715, 654, 68, 1044, 79, 405, 67, 395, 468, 661, 651, 81, 845,
            340, 653, 397, 392, 1236, 80, 262, 20, 522, 134, 138, 71, 69, 130, 399, 266, 260, 655, 917, 401, 198, 718,
            202, 657, 1, 520, 400, 83, 846, 725, 975, 656, 596, 717, 853, 391, 1105, 270, 326, 142, 195, 526, 330, 458,
            533, 149, 267, 911, 277, 981, 719, 403, 525, 139, 913, 6, 269, 73, 523, 1040, 1109, 213, 206, 141, 659, 203,
            912, 721, 10, 847, 264, 132, 205, 1045, 334, 849, 462, 271, 720, 527, 196, 341, 848, 469, 529, 273, 136,
            393, 331, 145, 459, 461, 455, 977, 325, 915, 131, 143, 528, 586, 14, 21, 1237, 209, 144, 272, 333, 723, 11,
            976, 328, 200, 208, 207, 261, 135, 851, 463, 133, 13, 263, 1041, 456, 335, 197, 1235, 199, 8, 465, 4, 531,
            590, 275, 147, 337, 597, 2, 0, 587, 979, 464, 336, 15, 211, 589, 3, 1107, 17, 327, 1043, 16, 521, 137, 265,
            591, 7, 467, 339, 593, 201, 5, 592, 585, 19, 329, 457, 595, 9
    };
    private static final Map<Integer, Integer> INFORMATIVE_LOOKUP = IntStream.range(0, SORTED_RESIDUE_TYPE_COMBINATIONS.length)
            .mapToObj(i -> new Pair<>(SORTED_RESIDUE_TYPE_COMBINATIONS[i], i))
            .collect(Collectors.toMap(Pair::first, Pair::second));

    private static int getInformativeness(int descriptor) {
        int residueCombination = descriptor >>> 16 & 0xFFF;
        return INFORMATIVE_LOOKUP.getOrDefault(residueCombination, DEFAULT_INFORMATIVENESS);
    }

    /**
     * Tries to sort occurrences so that the most informative ones occur first. Not optimized.
     */
    public static final Comparator<? super ResiduePairOccurrence> INFORMATIVENESS_COMPARATOR =
            Comparator.comparingInt((ResiduePairOccurrence o) -> ResiduePairDescriptor.getBackboneDistance(o.getResiduePairDescriptor()).ordinal())
                    .thenComparing(o -> getInformativeness(o.getResiduePairDescriptor()));
}
