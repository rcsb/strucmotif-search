package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

class StructureReaderImplTest {
    private StructureReader structureReader;

    @BeforeEach
    public void init() {
        this.structureReader = new StructureReaderImpl(new ResidueTypeResolverImpl(new StrucmotifConfig()));
    }

    @Test
    void whenReadingRNA_thenAssembliesParsedCorrectly() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("7els"));
        assertEquals(2, structure.getAssemblies()
                .values()
                .stream()
                .flatMap(Arrays::stream)
                .map(oper -> oper.split("_")[1])
                .distinct()
                .count());
    }

    @Test
    void whenAssemblySelection_thenReturnNoDuplicates() {
        // e.g. for 3vk6 (A_2-61, A_1-80, A_1-85) will return 6 residues
        Collection<LabelSelection> selection = List.of(new LabelSelection("A", "1", 61), new LabelSelection("A", "1", 80), new LabelSelection("A", "1", 85));
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3vk6"));
        List<Map<LabelAtomId, float[]>> residues = selection.stream()
                .map(structure::manifestResidue)
                .collect(Collectors.toList());
        assertTrue(residues.size() < 4, "contains duplicates");
        assertEquals(3, residues.size());
    }

    @Test
    void whenMicroheterogeneityAtSequenceLevelInRenumberedFile_thenReportCorrectResidueType() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1eta"));
        int aIndex = structure.getResidueIndex("A",30);
        int bIndex = structure.getResidueIndex("B", 30);
        ResidueType a30Type = structure.getResidueType(aIndex);
        ResidueType b30Type = structure.getResidueType(bIndex);

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
        assertEquals(6, chainCount(structure));
    }

    @Test
    void whenDuplicatedChainsAndIdentityOperationsInRenumberedFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));
        assertEquals(4, chainCount(structure));
    }

    @Test
    void whenMicroheterogeneityInRenumberedFile_thenAtomCountMatches() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(new LabelSelection("A", "1", 249));
        // should report all unique atom names, but won't report non-standard OD
        assertEquals(6, residue.size());
    }

    @Test
    void whenProcessing1exrInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1exr"));
        assertEquals(1, chainCount(structure));
        assertEquals(146, residueCount(structure));
        assertEquals(1150, atomCount(structure));

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
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(new LabelSelection("A", "1", 9));
        float[] c1 = residue.values().stream().findFirst().orElseThrow();
        assertArrayEquals(new float[] { 50.7f, 12.1f, 7.6f }, c1, Helpers.DELTA);
        float[] c2 = residue.values().stream().skip(1).findFirst().orElseThrow();
        assertArrayEquals(new float[] { 49.8f, 12.9f, 8.4f }, c2, Helpers.DELTA);
    }

    @Test
    void whenProcessingStructureWithSymmetryInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1acj"));
        int multiplier = structure.getTransformations().size();
        assertEquals(2, multiplier * chainCount(structure));
        assertEquals(1056, multiplier * residueCount(structure));
        assertEquals(8190, multiplier * atomCount(structure));
    }

    @Test
    void whenProcessingStructureWithAssemblyInRenumberedFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("1m4x"));
        int multiplier = structure.getTransformations().size();
        assertEquals(5208, multiplier * chainCount(structure));
        assertEquals(5208 * 413, multiplier * residueCount(structure));
        assertEquals(5208 * 3231, multiplier * atomCount(structure));
    }

    @Test
    void whenMicroheterogeneityAtSequenceLevelInOriginalFile_thenReportCorrectResidueType() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1eta"));
        int aIndex = structure.getResidueIndex("A",  30);
        int bIndex = structure.getResidueIndex("B", 30);
        ResidueType a30Type = structure.getResidueType(aIndex);
        ResidueType b30Type = structure.getResidueType(bIndex);

        assertEquals(ResidueType.METHIONINE, a30Type, "wrong type due to microheterogeneity");
        assertEquals(ResidueType.METHIONINE, b30Type, "wrong type due to microheterogeneity");
    }

    @Test
    void whenNoModel1InOriginalFile_thenIgnoreModelInfoAndReturnChains() {
        // multiple NMR models distributed over multiple structures - file will start with model nr 18
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1ezc"));
        assertEquals(1, chainCount(structure));
    }

    @Test
    void whenProcessing4chaInOriginalFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4cha"));

        assertEquals(11, chainCount(structure));
        assertEquals(482, residueCount(structure));
        assertEquals(3591, atomCount(structure));
    }

    @Test
    void whenDuplicatedChainsAndIdentityOperationsInOriginalFile_thenChainCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("3uud"));

        assertEquals(14, chainCount(structure));
        assertEquals(509, residueCount(structure));
        assertEquals(4459, atomCount(structure));
    }

    @Test
    void whenMicroheterogeneityInOriginalFile_thenAtomCountMatches() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Map<LabelAtomId, float[]> residue = structure.manifestResidue(new LabelSelection("A", "1", 249));
        // should report all unique atom names, but won't report non-standard OD
        assertEquals(6, residue.size());
    }

    @Test
    void whenProcessing1exrInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1exr"));

        assertEquals(7, chainCount(structure));
        assertEquals(152, residueCount(structure));
        assertEquals(1650, atomCount(structure));
    }

    @Test
    void whenProcessingStructureWithSymmetryInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1acj"));
        int multiplier = structure.getTransformations().size();

        assertEquals(6, multiplier * chainCount(structure));
        assertEquals(1060, multiplier * residueCount(structure));
        assertEquals(8384, multiplier * atomCount(structure));
    }

    @Test
    void whenProcessingStructureWithAssemblyInOriginalFile_thenCountsMatch() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("1m4x"));
        int multiplier = structure.getTransformations().size();

        assertEquals(5208, multiplier * chainCount(structure));
        assertEquals(5208 * 413, multiplier * residueCount(structure));
        assertEquals(5208 * 3231, multiplier * atomCount(structure));
    }

    private long chainCount(Structure structure) {
        return structure.getLabelSelections()
                .stream()
                .map(LabelSelection::getLabelAsymId)
                .distinct()
                .count();
    }

    private long residueCount(Structure structure) {
        return structure.getResidueCount();
    }

    private long atomCount(Structure structure) {
        return structure.getAtomCount();
    }
}