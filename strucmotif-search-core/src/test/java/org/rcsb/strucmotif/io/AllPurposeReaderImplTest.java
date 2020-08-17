package org.rcsb.strucmotif.io;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rcsb.strucmotif.GuiceJUnit4Runner;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.AllPurposeReader;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(GuiceJUnit4Runner.class)
public class AllPurposeReaderImplTest {
    @Inject
    private AllPurposeReader allPurposeReader;

    @Test
    public void shouldHandleMicroheterogeneityAtSequenceLevel() {
        Structure structure = allPurposeReader.readById("1eta");
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
        assertEquals("duplicated positions due to microheterogeneity", 1, chainA30.size());
        assertEquals("wrong type due to microheterogeneity", ResidueType.METHIONINE, chainA30.iterator().next().getResidueIdentifier().getResidueType());
        assertEquals("duplicated positions due to microheterogeneity", 1, chainB30.size());
        assertEquals("wrong type due to microheterogeneity", ResidueType.METHIONINE, chainB30.iterator().next().getResidueIdentifier().getResidueType());
    }

    @Test
    public void shouldSkipNmrModelsWithNumberUnequalToOne() {
        // multiple NMR models distributed over multiple structures - file will start with model nr 18
        Structure structure = allPurposeReader.readById("1ezc");
        assertEquals(0, chainCount(structure));
    }

    @Test
    public void shouldBuildAndWriteAssemblyForVirusParticle() throws IOException {
        Structure structure = allPurposeReader.readById("2bfu");
        assertEquals(2 * 60, chainCount(structure));

        new GenericTextStructureWriter().write(structure, Paths.get("target/test_ap.cif"));
    }

    @Test
    public void shouldHandle4cha() {
        Structure structure = allPurposeReader.readById("4cha");

        assertEquals(11, chainCount(structure));
        assertEquals(562, entityCount(structure));
        assertEquals(3591, atomCount(structure));
    }

    @Test
    public void shouldBuildAssembliesForDoubleIdentityOperatorAndDuplicatedChains() {
        Structure structure = allPurposeReader.readById("3uud");

        assertEquals(14, chainCount(structure));
        assertEquals(883, entityCount(structure));
        assertEquals(4459, atomCount(structure));
    }

    @Test
    public void shouldHandleMicroheterogeneity() {
        Structure structure = allPurposeReader.readById("2bwx");
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
    public void shouldCreateTrivialStructure() {
        Structure structure = allPurposeReader.readById("1exr");

        assertEquals(7, chainCount(structure));
        assertEquals(329, entityCount(structure));
        assertEquals(1650, atomCount(structure));
    }

    @Test
    public void shouldCreateStructureWithSymmetry() {
        Structure structure = allPurposeReader.readById("1acj");

        assertEquals(2 * 3, chainCount(structure));
        assertEquals(2 * (528 + 1 + 82), entityCount(structure));
        assertEquals(2 * 4192, atomCount(structure));
    }

    @Test
    public void shouldCreateStructureWithBioAssemblies() {
        Structure structure = allPurposeReader.readById("1m4x");

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