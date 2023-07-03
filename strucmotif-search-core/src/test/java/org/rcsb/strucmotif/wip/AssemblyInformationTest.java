package org.rcsb.strucmotif.wip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;
import static org.rcsb.strucmotif.Helpers.getRenumberedBcif;

class AssemblyInformationTest {
    private DefaultStructureReader structureReader;

    @BeforeEach
    public void init() {
        this.structureReader = new DefaultStructureReader(new ResidueTypeResolverImpl(new StrucmotifConfig()));
    }

    private static final String DATA_1M4X = """
            data_1M4X
            #
            _entry.id   1M4X
            #
            loop_
            _pdbx_struct_assembly.id
            _pdbx_struct_assembly.details
            _pdbx_struct_assembly.method_details
            _pdbx_struct_assembly.oligomeric_details
            _pdbx_struct_assembly.oligomeric_count
            1 'complete icosahedral assembly'                ? 'complete icosahedral assembly' 5040
            2 'icosahedral asymmetric unit'                  ? 84-meric                        84
            3 'icosahedral pentamer'                         ? 420-meric                       420
            4 'icosahedral 23 hexamer'                       ? 504-meric                       504
            5 'pentasymmetron capsid unit'                   ? 90-meric                        90
            6 'trisymmetron capsid unit'                     ? 198-meric                       198
            7 'icosahedral asymmetric unit, std point frame' ? 84-meric                        84
            #
            loop_
            _pdbx_struct_assembly_gen.assembly_id
            _pdbx_struct_assembly_gen.oper_expression
            _pdbx_struct_assembly_gen.asym_id_list
            1 '(1-60)(61-88)'           A,B,C
            2 '(61-88)'                 A,B,C
            3 '(1-5)(61-88)'            A,B,C
            4 '(1,2,6,10,23,24)(61-88)' A,B,C
            5 '(1-5)(63-68)'            A,B,C
            6 '(1,10,23)(61,62,69-88)'  A,B,C
            7 '(P)(61-88)'              A,B,C
            #
            """;
    private static final String DATA_1M4X_ALL_CANDIDATES = """
            data_1M4X
            #
            _entry.id   1M4X
            #
            loop_
            _pdbx_struct_assembly.id
            _pdbx_struct_assembly.details
            _pdbx_struct_assembly.method_details
            _pdbx_struct_assembly.oligomeric_details
            _pdbx_struct_assembly.oligomeric_count
            1 'complete icosahedral assembly' ? 'complete icosahedral assembly' 5040
            2 'complete icosahedral assembly' ? 84-meric                        84
            3 'complete icosahedral assembly' ? 420-meric                       420
            4 'complete icosahedral assembly' ? 504-meric                       504
            5 'complete icosahedral assembly' ? 90-meric                        90
            6 'complete icosahedral assembly' ? 198-meric                       198
            7 'complete icosahedral assembly' ? 84-meric                        84
            #
            loop_
            _pdbx_struct_assembly_gen.assembly_id
            _pdbx_struct_assembly_gen.oper_expression
            _pdbx_struct_assembly_gen.asym_id_list
            1 '(1-60)(61-88)'           A,B,C
            2 '(61-88)'                 A,B,C
            3 '(1-5)(61-88)'            A,B,C
            4 '(1,2,6,10,23,24)(61-88)' A,B,C
            5 '(1-5)(63-68)'            A,B,C
            6 '(1,10,23)(61,62,69-88)'  A,B,C
            7 '(P)(61-88)'              A,B,C
            #
            """;
    private static final String DATA_5CBG = """
            data_5CBG
            #
            _entry.id   5CBG
            #
            loop_
            _pdbx_struct_assembly.id
            _pdbx_struct_assembly.details
            _pdbx_struct_assembly.method_details
            _pdbx_struct_assembly.oligomeric_details
            _pdbx_struct_assembly.oligomeric_count
            1 author_and_software_defined_assembly PISA tetrameric 4
            2 author_and_software_defined_assembly PISA tetrameric 4
            3 author_and_software_defined_assembly PISA tetrameric 4
            #
            loop_
            _pdbx_struct_assembly_gen.assembly_id
            _pdbx_struct_assembly_gen.oper_expression
            _pdbx_struct_assembly_gen.asym_id_list
            1 1       A,B,D,E,G,H,J,K,L,M,R,S,U,V
            2 1,2,3,4 C,I,T
            3 1,2,3,4 F,N,O,P,Q,W
            #""";
    private static final String DATA_AFA0A009IHW8F1 = """
            data_AF_AFA0A009IHW8F1
            #
            _entry.id   AF_AFA0A009IHW8F1
            #""";

    @Test
    void whenFirstAssemblyIsCandidateAndTransformations_thenRecordReturned() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        DefaultStructureReader.AssemblyInformation assemblyInfo = structureReader.parseAssemblies(block, List.of("A", "B", "C"));
        assertNotNull(assemblyInfo);
        assertArrayEquals(new String[] { "1" }, assemblyInfo.assemblyIdentifiers());
        assertArrayEquals(new int[] { 0 }, assemblyInfo.assemblyOffsets());
        assertEquals("A", assemblyInfo.assemblyReferences()[0]);
        assertEquals("1x61", assemblyInfo.assemblyReferences()[1]);
        assertEquals("B", assemblyInfo.assemblyReferences()[2]);
        assertEquals("1x61", assemblyInfo.assemblyReferences()[3]);
        assertEquals("C", assemblyInfo.assemblyReferences()[4]);
        assertEquals("1x61", assemblyInfo.assemblyReferences()[5]);
    }

    @Test
    void whenMultipleCandidates5cbg_thenMultipleMapEntries() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_5CBG.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        DefaultStructureReader.AssemblyInformation assemblyInfo = structureReader.parseAssemblies(block, List.of("A", "B", "C", "D", "E", "F"));
        assertNotNull(assemblyInfo);
        assertEquals(3, assemblyInfo.assemblyIdentifiers().length);
    }

    @Test
    void whenSingleCandidate1m4x_thenMultipleMapEntries() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        DefaultStructureReader.AssemblyInformation assemblyInfo = structureReader.parseAssemblies(block, List.of("A", "B", "C"));
        assertNotNull(assemblyInfo);
        assertEquals(1, assemblyInfo.assemblyIdentifiers().length);
    }

    @Test
    void whenMultipleCandidates1m4x_thenMultipleMapEntries() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X_ALL_CANDIDATES.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        DefaultStructureReader.AssemblyInformation assemblyInfo = structureReader.parseAssemblies(block, List.of("A", "B", "C"));
        assertNotNull(assemblyInfo);
        assertEquals(7, assemblyInfo.assemblyIdentifiers().length);
    }

    @Test
    void whenNoAssemblyInformation_thenAssemblyReferencesInferred() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_AFA0A009IHW8F1.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        DefaultStructureReader.AssemblyInformation assemblyInfo = structureReader.parseAssemblies(block, List.of("A"));
        assertArrayEquals(new String[] { "A", "1" }, assemblyInfo.assemblyReferences());
    }

    @Test
    void whenFirstAssemblyIsCandidate_thenCandidatesNull() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = structureReader.getAssemblyCandidates(block);
        assertEquals(Set.of("1"), candidates);
    }

    @Test
    void whenMultipleCandidates5cbg_thenReturnAllCandidates() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_5CBG.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = structureReader.getAssemblyCandidates(block);
        assertEquals(Set.of("1", "2", "3"), candidates);
    }

    @Test
    void whenMultipleCandidates1m4x_thenReturnAllCandidates() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X_ALL_CANDIDATES.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = structureReader.getAssemblyCandidates(block);
        assertEquals(Set.of("1", "2", "3", "4", "5", "6", "7"), candidates);
    }

    @Test
    void whenNoAssemblyInformation_thenCandidatesEmpty() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_AFA0A009IHW8F1.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = structureReader.getAssemblyCandidates(block);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void whenInterleavingAssemblies_thenReferencesResolvedCorrectly() {
        Structure structure = structureReader.readFromInputStream(getRenumberedBcif("3uud"));
        assertArrayEquals(new String[] { "1", "2", "3" }, structure.getAssemblyIdentifiers());
        assertArrayEquals(new String[] { "A", "1", "B", "1", "C", "1", "D", "1" }, structure.getReferencedChainInstances("1"));
        assertArrayEquals(new String[] { "A", "1", "C", "1" }, structure.getReferencedChainInstances("2"));
        assertArrayEquals(new String[] { "B", "1", "D", "1" }, structure.getReferencedChainInstances("3"));
        assertArrayEquals(new String[] { "1" }, structure.getTransformationIdentifiers());
        assertArrayEquals(new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 }, structure.getTransformations());
    }

    @Test
    void whenTrivialCase_thenDefaultAssemblyInformationAvailable() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("200l"));
        assertArrayEquals(new String[] { "1" }, structure.getAssemblyIdentifiers());
        // this contains all chains, including water
        assertArrayEquals(new String[] { "A", "1", "B", "1", "C", "1", "D", "1", "E", "1", "F", "1" }, structure.getReferencedChainInstances("1"));
        assertArrayEquals(new String[] { "1" }, structure.getTransformationIdentifiers());
        assertArrayEquals(new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 }, structure.getTransformations());
    }
}