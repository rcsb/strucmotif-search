package org.rcsb.strucmotif.io.read;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.GenericTextStructureWriter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

public class RenumberedReaderImplTest {
    private RenumberedReader renumberedReader;

    @BeforeEach
    public void init() {
        this.renumberedReader = new RenumberedReaderImpl(new MotifSearchConfig());
    }

    @Test
    public void whenMicroheterogeneityAtSequenceLevel_thenReportCorrectResidueType() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("1eta"));
        Chain chainA = structure.getChains().get(0);
        Chain chainB = structure.getChains().get(1);
        Set<Residue> chainA30 = chainA.getResidues()
                .stream()
                .filter(component -> component.getResidueIdentifier().getLabelSeqId() == 30)
                .collect(Collectors.toSet());
        Set<Residue> chainB30 = chainB.getResidues()
                .stream()
                .filter(component -> component.getResidueIdentifier().getLabelSeqId() == 30)
                .collect(Collectors.toSet());
        assertEquals(1, chainA30.size(), "duplicated positions due to microheterogeneity");
        assertEquals(ResidueType.METHIONINE, chainA30.iterator().next().getResidueIdentifier().getResidueType(), "wrong type due to microheterogeneity");
        assertEquals(1, chainB30.size(), "duplicated positions due to microheterogeneity");
        assertEquals(ResidueType.METHIONINE, chainB30.iterator().next().getResidueIdentifier().getResidueType(), "wrong type due to microheterogeneity");
    }

    @Test
    public void whenNoModel1_thenThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            // multiple NMR models distributed over multiple structures - file will start with model nr 18
            Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("1ezc"));
            assertEquals(0, structure.getChains().size());
        });
    }

    @Test
    public void whenVirusParticle_thenWriteAssemblyCoordinates() throws IOException {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("2bfu"));
        assertEquals(120, structure.getChains().size());

        new GenericTextStructureWriter().write(structure, Paths.get("target/test_renum.cif"));
    }

    @Test
    public void whenProcessing4cha_thenChainCountMatches() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("4cha"));
        assertEquals(6, structure.getChains().size());
    }

    @Test
    public void whenDuplicatedChainsAndIdentityOperations_thenChainCountMatches() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("3uud"));
        assertEquals(4, structure.getChains().size());
    }

    @Test
    public void whenMicroheterogeneity_thenAtomCountMatches() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Residue residue = structure.getChains()
                .get(0)
                .getResidues()
                .stream()
                .filter(c -> c.getResidueIdentifier().getLabelSeqId() == 249)
                .findFirst()
                .orElseThrow();
        // should report all unique atom names
        assertEquals(7, residue.getAtoms().size());
        // should report OD only present in hetatm
        assertTrue(residue.getAtoms().stream().anyMatch(atom -> atom.getAtomIdentifier().getLabelAtomId().equals("OD")));
    }

    @Test
    public void whenProcessing1exr_thenCountsMatch() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("1exr"));
        assertEquals(1, chainCount(structure));
        assertEquals(146, componentCount(structure));
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
        Residue residue = structure.getChains().get(0).getResidues().get(7);
        Atom atomN = residue.getAtoms().get(0);
        assertArrayEquals(new double[] { 50.0, 12.6, 9.9 }, atomN.getCoord(), Helpers.DELTA);
        Atom atomCA = residue.getAtoms().get(1);
        assertArrayEquals(new double[] { 49.8, 12.9, 8.4 }, atomCA.getCoord(), Helpers.DELTA);
    }

    @Test
    public void whenProcessingStructureWithSymmetry_thenCountsMatch() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("1acj"));
        assertEquals(2, chainCount(structure));
        assertEquals(1056, componentCount(structure));
        assertEquals(8190, atomCount(structure));
    }

    @Test
    public void whenProcessingStructureWithAssembly_thenCountsMatch() {
        Structure structure = renumberedReader.readFromInputStream(getRenumberedBcif("1m4x"));
        assertEquals(5040, chainCount(structure));
        assertEquals(3 * 1680 * 413, componentCount(structure));
        assertEquals(3 * 1680 * 3231, atomCount(structure));
    }

    private long chainCount(Structure structure) {
        return structure.getChains().size();
    }

    private long componentCount(Structure structure) {
        return structure.getChains()
                .stream()
                .map(Chain::getResidues)
                .mapToLong(Collection::size)
                .sum();
    }

    private long atomCount(Structure structure) {
        return structure.getChains()
                .stream()
                .map(Chain::getResidues)
                .flatMap(Collection::stream)
                .map(Residue::getAtoms)
                .mapToLong(Collection::size)
                .sum();
    }
}
