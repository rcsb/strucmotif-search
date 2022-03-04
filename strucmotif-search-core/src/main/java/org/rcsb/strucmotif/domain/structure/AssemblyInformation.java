package org.rcsb.strucmotif.domain.structure;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Assembly definitions for a structure.
 */
public class AssemblyInformation {
    /**
     * Construct assembly information instance from source file.
     * @param mmCifFile source file
     * @return a map, keys are assembly identifiers, values are associated label_asym_id + struct_oper_id
     */
    public static Map<String, String[]> of(MmCifFile mmCifFile) {
        /*
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
         */
        PdbxStructAssemblyGen pdbxStructAssemblyGen = mmCifFile.getFirstBlock().getPdbxStructAssemblyGen();
        Map<String, Set<String>> assemblyInformation = new HashMap<>();
        if (pdbxStructAssemblyGen.isDefined()) {
            for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
                String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
                String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
                String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
                List<String> operList = getOperList(operExpression, asymIdList);

                Set<String> sorted = assemblyInformation.computeIfAbsent(assemblyId, e -> new HashSet<>());
                sorted.addAll(operList);
            }
        }
        return assemblyInformation.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toArray(String[]::new)));
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private static final Pattern LIST_PATTERN = Pattern.compile(",");
    private static List<String> getOperList(String operExpression, String asymIdList) {
        List<String> operations = new ArrayList<>();
        List<String> chains = LIST_PATTERN.splitAsStream(asymIdList).collect(Collectors.toList());
        String[] split = OPERATION_PATTERN.split(operExpression);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    for (String chain : chains) {
                        operations.add(chain + "_" + id1 + "x" + id2);
                    }
                }
            }
        } else {
            for (String id : extractTransformationIds(operExpression)) {
                for (String chain : chains) {
                    operations.add(chain + "_" + id);
                }
            }
        }

        return operations;
    }

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static List<String> extractTransformationIds(String rawOperation) {
        String prepared = rawOperation.replace("(", "")
                .replace(")", "")
                .replace("'", "");

        return COMMA_PATTERN.splitAsStream(prepared)
                .flatMap(AssemblyInformation::extractTransformationRanges)
                .collect(Collectors.toList());
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("-");
    private static Stream<String> extractTransformationRanges(String raw) {
        String[] s = RANGE_PATTERN.split(raw);
        if (s.length == 1) {
            return Stream.of(raw);
        } else {
            return IntStream.range(Integer.parseInt(s[0]), Integer.parseInt(s[1]) + 1)
                    .mapToObj(String::valueOf);
        }
    }
}
