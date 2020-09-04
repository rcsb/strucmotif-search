package org.rcsb.strucmotif.io.read;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class AllPurposeReaderImplTest {
    private AllPurposeReader allPurposeReader;

    @BeforeEach
    public void init() {
        this.allPurposeReader = new AllPurposeReaderImpl();
    }

    // TODO 4udf seems slow

    @Test
    public void whenMicroheterogeneityAtSequenceLevel_thenReportCorrectResidueType() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("1eta"));
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
    public void whenNoModel1_thenReturnNoChains() {
        // multiple NMR models distributed over multiple structures - file will start with model nr 18
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("1ezc"));
        assertEquals(0, chainCount(structure));
    }

    @Test
    public void whenVirusParticle_thenWriteAssemblyCoordinates() throws IOException {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("2bfu"));
        assertEquals(2 * 60, chainCount(structure));

        new GenericTextStructureWriter().write(structure, Paths.get("target/test_ap.cif"));
    }

    @Test
    public void whenProcessing4cha_thenChainCountMatches() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("4cha"));

        assertEquals(6, chainCount(structure));
        assertEquals(477, entityCount(structure));
        assertEquals(3506, atomCount(structure));
    }

    @Test
    public void whenDuplicatedChainsAndIdentityOperations_thenChainCountMatches() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("3uud"));

        assertEquals(4, chainCount(structure));
        assertEquals(499, entityCount(structure));
        assertEquals(4013, atomCount(structure));
    }

    @Test
    public void whenMicroheterogeneity_thenAtomCountMatches() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("2bwx"));
        // group contains alt locs and microheterogeneity
        Residue residue = structure.getChains()
                .get(0)
                .getResidues()
                .stream()
                .filter(c -> c.getResidueIdentifier().getLabelSeqId() == 249)
                .findFirst()
                .orElseThrow();
        // should report all atoms in all altlocs
        assertEquals(15, residue.getAtoms().size());
        // should report OD only present in hetatm
        assertTrue(residue.getAtoms().stream().anyMatch(atom -> atom.getAtomIdentifier().getLabelAtomId().equals("OD")));
    }

    @Test
    public void whenProcessing1exr_thenCountsMatch() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("1exr"));

        assertEquals(1, chainCount(structure));
        assertEquals(146, entityCount(structure));
        assertEquals(1467, atomCount(structure));
    }

    @Test
    public void whenProcessingStructureWithSymmetry_thenCountsMatch() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("1acj"));

        assertEquals(2, chainCount(structure));
        assertEquals(1056, entityCount(structure));
        assertEquals(8190, atomCount(structure));
    }

    @Test
    public void whenProcessingStructureWithAssembly_thenCountsMatch() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("1m4x"));

        assertEquals(3 * 1680, chainCount(structure));
        assertEquals(3 * 1680 * 413, entityCount(structure));
        assertEquals(3 * 1680 * 3231, atomCount(structure));
    }

    private long chainCount(Structure structure) {
        return structure.getChains().size();
    }

    private long entityCount(Structure structure) {
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