package org.rcsb.strucmotif;

import org.mockito.invocation.InvocationOnMock;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Helpers {
    public static final double DELTA = 0.001;

    @SuppressWarnings("unchecked")
    public static Structure mockStructureDataProviderReadRenumbered(InvocationOnMock invocation) {
        StructureIdentifier structureIdentifier = invocation.getArgument(0, StructureIdentifier.class);
        Collection<IndexSelection> selection = (Collection<IndexSelection>) invocation.getArgument(1, Collection.class);

        Map<String, String> structureData = new BufferedReader(new InputStreamReader(getResource("structures.data")))
                .lines()
                .map(line -> line.replace("]", "").split("\\["))
                .collect(Collectors.toMap(split -> split[0], split -> split[1]));

        Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

        int aIndex = 0;
        for (IndexSelection indexSelection : selection) {
            String key = structureIdentifier.getPdbId() + ":" + indexSelection.getStructOperId() + ":" + indexSelection.getIndex();
            String res = structureData.get(key);

            String[] split = res.split(", ");
            String chainId = split[0];
            int seqId = Integer.parseInt(split[1]);
            ChainIdentifier chainIdentifier = new ChainIdentifier(chainId, indexSelection.getStructOperId());
            ResidueType residueType = ResidueType.ofOneLetterCode(split[2]);
            ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, seqId, indexSelection.getIndex());
            List<Atom> atoms = new ArrayList<>();
            for (int i = 3; i < split.length; i = i + 4) {
                String name = split[i];
                double[] coord = new double[] {
                        (Integer.parseInt(split[i + 1])) * 0.1,
                        (Integer.parseInt(split[i + 2])) * 0.1,
                        (Integer.parseInt(split[i + 3])) * 0.1
                };
                atoms.add(StructureFactory.createAtom(new AtomIdentifier(name, ++aIndex), coord));
            }
            Residue residue = StructureFactory.createResidue(residueIdentifier, atoms, Matrix4DTransformation.IDENTITY_MATRIX_4D);
            tmp.computeIfAbsent(chainIdentifier, c -> new ArrayList<>()).add(residue);
        }

        List<Chain> chains = tmp.entrySet()
                .stream()
                .map(entry -> StructureFactory.createChain(entry.getKey(), entry.getValue(), Matrix4DTransformation.IDENTITY_MATRIX_4D))
                .collect(Collectors.toList());
        return StructureFactory.createStructure(structureIdentifier, chains);
    }

    public static Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> mockInvertedIndexSelect(InvocationOnMock invocation) {
        ResiduePairDescriptor residuePairDescriptor = invocation.getArgument(0, ResiduePairDescriptor.class);

        try {
            InputStream inputStream = getResource("index/" + residuePairDescriptor.toString() + ".data");
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .map(line -> handleLine(line, residuePairDescriptor));
        } catch (NullPointerException e) {
            // allowed to happen when empty bins are requested during operations with tolerance
            return Stream.empty();
        }
    }

    private static Pair<StructureIdentifier, ResiduePairIdentifier[]> handleLine(String line, ResiduePairDescriptor residuePairDescriptor) {
        String[] outerSplit = line.split(":");
        StructureIdentifier key = new StructureIdentifier(outerSplit[0]);
        ResiduePairIdentifier[] value = new ResiduePairIdentifier[outerSplit.length - 1];
        for (int i = 1; i < outerSplit.length; i++) {
            String[] innerSplit = outerSplit[i].split(",");
            int index1 = Integer.parseInt(innerSplit[0]);
            int index2 = Integer.parseInt(innerSplit[1]);
            String structOperId1 = "1";
            String structOperId2 = "1";
            if (innerSplit.length == 4) {
                structOperId1 = innerSplit[2];
                structOperId2 = innerSplit[3];
            }
            IndexSelection indexSelection1 = new IndexSelection(structOperId1, index1);
            IndexSelection indexSelection2 = new IndexSelection(structOperId2, index2);
            if (flipped) {
                value[i - 1] = new ResiduePairIdentifier(indexSelection2, indexSelection1);
            IndexSelection indexSelection1 = new IndexSelection(assemblyId1, index1);
            IndexSelection indexSelection2 = new IndexSelection(assemblyId2, index2);
            if (residuePairDescriptor.isFlipped()) {
                value[i - 1] = new ResiduePairIdentifier(indexSelection2, indexSelection1, residuePairDescriptor);
            } else {
                value[i - 1] = new ResiduePairIdentifier(indexSelection1, indexSelection2, residuePairDescriptor);
            }
        }
        return new Pair<>(key, value);
    }

    public static InputStream getOriginalBcif(String pdbId) {
        return getResource("orig/" + pdbId + ".bcif");
    }

    public static InputStream getRenumberedBcif(String pdbId) {
        return getResource("bcif/" + pdbId + ".bcif");
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
                            AngleType.values()[ik],
                            residuePairDescriptor));
                }
            }
        }

        return combinations.stream();
    }
}
