package org.rcsb.strucmotif.wip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

class DefaultStructureReaderTest {
    private DefaultStructureReader structureReader;

    @BeforeEach
    public void init() {
        this.structureReader = new DefaultStructureReader(new ResidueTypeResolverImpl(new StrucmotifConfig()));
    }

    @Test
    void whenReadingRNA_thenAssembliesParsedCorrectly() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7els"));
        String[] refs = structure.getReferencedChainInstances("1");
        assertEquals(2, IntStream.range(0, refs.length / 2)
                .map(i -> 2 * i + 1)
                .mapToObj(i -> refs[i])
                .distinct()
                .count());
    }

    @Test
    void whenAssemblySelection_thenReturnNoDuplicates() {
        // e.g. for 3vk6 (A_2-61, A_1-80, A_1-85) will return 6 residues
        Collection<Integer> selection = List.of(60, 79, 84);
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3vk6"));
        List<Map<LabelAtomId, float[]>> residues = selection.stream()
                .map(structure::manifestResidue)
                .toList();
        assertTrue(residues.size() < 4, "contains duplicates");
        assertEquals(3, residues.size());
    }

    @Test
    void whenMicroheterogeneityAtSequenceLevelInRenumberedFile_thenReportCorrectResidueType() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1eta"));
        ResidueType a30Type = structure.getResidueType(29);
        ResidueType b30Type = structure.getResidueType(156);

        assertEquals(ResidueType.METHIONINE, a30Type, "wrong type due to microheterogeneity");
        assertEquals(ResidueType.METHIONINE, b30Type, "wrong type due to microheterogeneity");
    }

    @Test
    void whenNoModel1InRenumberedFile_thenThrowUnsupportedOperationException() {
        // multiple NMR models distributed over multiple structures - file will start with model nr 18
        assertThrows(UnsupportedOperationException.class, () -> structureReader.readFromInputStream(getRenumberedBcif("1ezc")));
    }

    @Test
    void whenProcessing4chaInRenumberedFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("4cha"));
        assertEquals(6, structure.getDepositedChainCount());
        assertEquals(6, structure.getInstancedChainCount());
    }

    @Test
    void whenDuplicatedChainsAndIdentityOperationsInRenumberedFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));
        assertEquals(4, structure.getDepositedChainCount());
        assertEquals(4, structure.getInstancedChainCount());
    }

    @Test
    void whenMicroheterogeneityInRenumberedFile_thenAtomCountMatches() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(248);
        // should report all unique atom names, but won't report non-standard OD
        assertEquals(6, residue.size());
    }

    @Test
    void whenProcessing1exrInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1exr"));
        assertEquals(1, structure.getDepositedChainCount());
        assertEquals(146, structure.getDepositedResidueCount());
        assertEquals(1150, structure.getDepositedAtomCount());

        // also has many alt locs
        /*
        ATOM   61   N  N   A ILE A 1 9   ? 50.042 12.619  9.863   0.60 15.47 ? 9    ILE A N   1
        ATOM   62   N  N   B ILE A 1 9   ? 50.064 12.669  9.955   0.40 15.79 ? 9    ILE A N   1
        ATOM   63   C  CA  A ILE A 1 9   ? 49.760 12.880  8.443   0.60 16.16 ? 9    ILE A CA  1
        ATOM   64   C  CA  B ILE A 1 9   ? 49.894 13.068  8.557   0.40 14.97 ? 9    ILE A CA  1
        ATOM   65   C  C   A ILE A 1 9   ? 50.740 12.096  7.568   0.60 16.68 ? 9    ILE A C   1
        ATOM   66   C  C   B ILE A 1 9   ? 50.739 12.182  7.629   0.40 15.01 ? 9    ILE A C   1
        ATOM   67   O  O   A ILE A 1 9   ? 50.344 11.527  6.549   0.60 13.30 ? 9    ILE A O   1
        ATOM   68   O  O   B ILE A 1 9   ? 50.215 11.661  6.634   0.40 13.83 ? 9    ILE A O   1
        ATOM   69   C  CB  A ILE A 1 9   ? 49.712 14.388  8.233   0.60 17.94 ? 9    ILE A CB  1
        ATOM   70   C  CB  B ILE A 1 9   ? 50.201 14.546  8.306   0.40 15.90 ? 9    ILE A CB  1
        ATOM   71   C  CG1 A ILE A 1 9   ? 48.529 15.065  8.881   0.60 19.71 ? 9    ILE A CG1 1
        ATOM   72   C  CG1 B ILE A 1 9   ? 49.111 15.466  8.868   0.40 14.83 ? 9    ILE A CG1 1
        ATOM   73   C  CG2 A ILE A 1 9   ? 49.644 14.632  6.712   0.60 18.98 ? 9    ILE A CG2 1
        ATOM   74   C  CG2 B ILE A 1 9   ? 50.339 14.857  6.814   0.40 13.92 ? 9    ILE A CG2 1
        ATOM   75   C  CD1 A ILE A 1 9   ? 48.524 16.563  9.024   0.60 23.19 ? 9    ILE A CD1 1
        ATOM   76   C  CD1 B ILE A 1 9   ? 49.568 16.907  8.941   0.40 21.29 ? 9    ILE A CD1 1
         */
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(7); // A-9
        float[] c1 = residue.values().stream().findFirst().orElseThrow();
        assertArrayEquals(new float[] { 50.7f, 12.1f, 7.6f }, c1, Helpers.DELTA);
        float[] c2 = residue.values().stream().skip(1).findFirst().orElseThrow();
        assertArrayEquals(new float[] { 49.8f, 12.9f, 8.4f }, c2, Helpers.DELTA);
    }

    @Test
    void whenProcessingStructureWithSymmetryInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1acj"));

        assertEquals(2, structure.getDepositedChainCount());
        assertEquals(1056, structure.getDepositedResidueCount());
        assertEquals(8190, structure.getDepositedAtomCount());
        assertEquals(2, structure.getInstancedChainCount()); // TODO need to fix renumbered files
        assertEquals(1056, structure.getInstancedResidueCount());
        assertEquals(8190, structure.getInstancedAtomCount());
    }

    @Test
    void whenProcessingStructureWithAssemblyInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1m4x"));

        assertEquals(3, structure.getDepositedChainCount());
        assertEquals(1239, structure.getDepositedResidueCount());
        assertEquals(9693, structure.getDepositedAtomCount());
        assertEquals(5040, structure.getInstancedChainCount()); // TODO need to fix renumbered files
        assertEquals(2081520, structure.getInstancedResidueCount());
        assertEquals(16284240, structure.getInstancedAtomCount());
    }

    @Test
    void whenMicroheterogeneityAtSequenceLevelInOriginalFile_thenReportCorrectResidueType() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1eta"));
        ResidueType a30Type = structure.getResidueType(29); // A-30
        ResidueType b30Type = structure.getResidueType(156); // B-30

        assertEquals(ResidueType.METHIONINE, a30Type, "wrong type due to microheterogeneity");
        assertEquals(ResidueType.METHIONINE, b30Type, "wrong type due to microheterogeneity");
    }

    @Test
    void whenNoModel1InOriginalFile_thenIgnoreModelInfoAndReturnChains() {
        // multiple NMR models distributed over multiple structures - file will start with model nr 18
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1ezc"));
        assertEquals(1, structure.getDepositedChainCount());
    }

    @Test
    void whenProcessing4chaInOriginalFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4cha"));

        assertEquals(11, structure.getDepositedChainCount());
        assertEquals(482, structure.getDepositedResidueCount());
        assertEquals(3591, structure.getDepositedAtomCount());
        assertEquals(11, structure.getInstancedChainCount());
        assertEquals(482, structure.getInstancedResidueCount());
        assertEquals(3591, structure.getInstancedAtomCount());
    }

    @Test
    void whenDuplicatedChainsAndIdentityOperationsInOriginalFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("3uud"));

        assertEquals(14, structure.getDepositedChainCount());
        assertEquals(509, structure.getDepositedResidueCount());
        assertEquals(4459, structure.getDepositedAtomCount());
        assertEquals(14, structure.getInstancedChainCount());
        assertEquals(509, structure.getInstancedResidueCount());
        assertEquals(4459, structure.getInstancedAtomCount());
    }

    @Test
    void whenMicroheterogeneityInOriginalFile_thenAtomCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(248);
        // should report all unique atom names, but won't report non-standard OD
        assertEquals(6, residue.size());
    }

    @Test
    void whenProcessing1exrInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1exr"));

        assertEquals(7, structure.getDepositedChainCount());
        assertEquals(152, structure.getDepositedResidueCount());
        assertEquals(1650, structure.getDepositedAtomCount());
        assertEquals(7, structure.getInstancedChainCount());
        assertEquals(152, structure.getInstancedResidueCount());
        assertEquals(1650, structure.getInstancedAtomCount());
    }

    @Test
    void whenProcessingStructureWithSymmetryInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1acj"));

        assertEquals(3, structure.getDepositedChainCount());
        assertEquals(528, structure.getDepositedResidueCount());
        assertEquals(4192, structure.getDepositedAtomCount());
        assertEquals(6, structure.getInstancedChainCount());
        assertEquals(1060, structure.getInstancedResidueCount());
        assertEquals(8384, structure.getInstancedAtomCount());
    }

    @Test
    void whenProcessingStructureWithAssemblyInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1m4x"));

        assertEquals(3, structure.getDepositedChainCount());
        assertEquals(1239, structure.getDepositedResidueCount());
        assertEquals(9693, structure.getDepositedAtomCount());
        assertEquals(5040, structure.getInstancedChainCount());
        assertEquals(2081520, structure.getInstancedResidueCount());
        assertEquals(16284240, structure.getInstancedAtomCount());
    }

    @Test
    void whenReadingStructureWithAmbiguousResidueType_thenSupported() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1kp0"));
        // GLX at position 5
        assertNotEquals(ResidueType.UNKNOWN_COMPONENT, structure.getResidueType(4));

//        ResidueGraph residueGraph = new ResidueGraph(structure, new StrucmotifConfig(), ResidueGraph.ResidueGraphOptions.deposited());
//        residueGraph.residuePairOccurrencesParallel()
//                .filter(p -> p.getResidueIdentifier().getIndex1() == 4 || p.getResidueIdentifier().getIndex2() == 4)
//                .map(ResiduePairOccurrence::getResiduePairDescriptor)
//                .forEach(d -> assertTrue(d.getResidueType1() == ResidueType.UNKNOWN_AMINO_ACID || d.getResidueType2() == ResidueType.UNKNOWN_AMINO_ACID));
    }

    @Test
    void whenReading1c3c_thenAssemblyChainsCorrectlySorted() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1c3c"));
        String[] refs = structure.getReferencedChainInstances("1");
        String[] fused = IntStream.range(0, refs.length / 2).map(i -> 2 * i).mapToObj(i -> refs[i] + "_" + refs[i + 1]).toArray(String[]::new);
        int i1 = 0;
        int i2 = 0;
        for (int i = 0; i < fused.length; i++) {
            String s = fused[i];
            if ("B_1".equals(s)) {
                i1 = i;
            }
            if ("B_2".equals(s)) {
                i2 = i;
            }
        }
        assertTrue(i1 < i2);
    }
}