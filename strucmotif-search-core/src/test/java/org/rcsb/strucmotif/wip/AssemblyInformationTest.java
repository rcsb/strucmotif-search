package org.rcsb.strucmotif.wip;

import org.junit.jupiter.api.Test;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifBlock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssemblyInformationTest {
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
    void whenFirstAssemblyIsCandidate_thenMapNull() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Map<String, List<String>> assemblyInfo = AssemblyInformation.of(block);
        assertNull(assemblyInfo);
    }

    @Test
    void whenMultipleCandidates5cbg_thenMultipleMapEntries() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_5CBG.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Map<String, List<String>> assemblyInfo = AssemblyInformation.of(block);
        assertNotNull(assemblyInfo);
        assertEquals(3, assemblyInfo.size());
        System.out.println(assemblyInfo);
    }

    @Test
    void whenMultipleCandidates1m4x_thenMultipleMapEntries() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X_ALL_CANDIDATES.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Map<String, List<String>> assemblyInfo = AssemblyInformation.of(block);
        assertNotNull(assemblyInfo);
        assertEquals(7, assemblyInfo.size());
        System.out.println(assemblyInfo);
    }

    @Test
    void whenNoAssemblyInformation_thenMapNull() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_AFA0A009IHW8F1.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Map<String, List<String>> assemblyInfo = AssemblyInformation.of(block);
        assertNull(assemblyInfo);
    }

    @Test
    void whenFirstAssemblyIsCandidate_thenCandidatesNull() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = AssemblyInformation.getAssemblyCandidates(block);
        assertNull(candidates);
    }

    @Test
    void whenMultipleCandidates5cbg_thenReturnAllCandidates() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_5CBG.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = AssemblyInformation.getAssemblyCandidates(block);
        assertEquals(Set.of("1", "2", "3"), candidates);
    }

    @Test
    void whenMultipleCandidates1m4x_thenReturnAllCandidates() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_1M4X_ALL_CANDIDATES.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = AssemblyInformation.getAssemblyCandidates(block);
        assertEquals(Set.of("1", "2", "3", "4", "5", "6", "7"), candidates);
    }

    @Test
    void whenNoAssemblyInformation_thenCandidatesNull() throws IOException {
        MmCifBlock block = CifIO.readFromInputStream(new ByteArrayInputStream(DATA_AFA0A009IHW8F1.getBytes(StandardCharsets.UTF_8)))
                .as(StandardSchemata.MMCIF)
                .getFirstBlock();
        Set<String> candidates = AssemblyInformation.getAssemblyCandidates(block);
        assertNull(candidates);
    }
}