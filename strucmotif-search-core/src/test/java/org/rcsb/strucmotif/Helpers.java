package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Helpers {
    public static final float DELTA = 0.001f;
    public static final float RELAXED_DELTA = 0.1f;

    public static short[] convertCoordsToShort(double[] array) {
        short[] out = new short[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (short) Math.round(array[i] * 10);
        }
        return out;
    }

    public static InputStream getOriginalBcif(String pdbId) {
        return getResource("orig/" + pdbId.toLowerCase() + ".bcif");
    }

    public static InputStream getRenumberedBcif(String pdbId) {
        return getResource("renum/" + pdbId + ".bcif");
    }

    public static InputStream getResource(String location) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
        return Objects.requireNonNull(resourceAsStream, "failed to locate test resource: " + location);
    }

    public static Stream<ResiduePairDescriptor> honorTolerance(ResiduePairDescriptor residuePairDescriptor) {
        int alphaCarbonDistanceTolerance = 1;
        int sideChainDistanceTolerance = 1;
        int angleTolerance = 1;

        int alphaCarbonDistance = residuePairDescriptor.getBackboneDistance().ordinal();
        int sideChainDistance = residuePairDescriptor.getSideChainDistance().ordinal();
        int dihedralAngle = residuePairDescriptor.getAngle().ordinal();
        List<ResiduePairDescriptor> combinations = new ArrayList<>();

        for (int i = -alphaCarbonDistanceTolerance; i <= alphaCarbonDistanceTolerance; i++) {
            int ii = alphaCarbonDistance + i;
            if (ii < 0 || ii >= DistanceType.values().length) {
                continue;
            }

            for (int j = -sideChainDistanceTolerance; j <= sideChainDistanceTolerance; j++) {
                int ij = sideChainDistance + j;
                if (ij < 0 || ij >= DistanceType.values().length) {
                    continue;
                }

                for (int k = -angleTolerance; k <= angleTolerance; k++) {
                    int ik = dihedralAngle + k;
                    if (ik < 0 || ik >= AngleType.values().length) {
                        continue;
                    }

                    combinations.add(new ResiduePairDescriptor(residuePairDescriptor.getResidueType1(),
                            residuePairDescriptor.getResidueType2(),
                            DistanceType.values()[ii],
                            DistanceType.values()[ij],
                            AngleType.values()[ik]));
                }
            }
        }

        return combinations.stream();
    }

    public static byte[] convertEnumToByte(Enum<?>... array) {
        byte[] out = new byte[array.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) array[i].ordinal();
        }
        return out;
    }

    public static List<LabelSelection> createLabelSelections(String labelAsymId, int... labelSeqIds) {
        return Arrays.stream(labelSeqIds)
                .mapToObj(i -> new LabelSelection(labelAsymId, null, i))
                .collect(Collectors.toList());
    }
}
