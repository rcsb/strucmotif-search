package org.rcsb.strucmotif.wip;

import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.PdbxStructAssembly;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AssemblyInformation {
    private final Map<String, String[]> assemblyInformation;

    private AssemblyInformation(Map<String, String[]> assemblyInformation) {
        this.assemblyInformation = assemblyInformation;
    }

    /**
     * Construct assembly information instance from source file. Returns <code>null</code> for the trivial case of a
     * single assembly.
     * @param block data container
     * @return a map, keys are assembly identifiers, values are associated label_asym_id and struct_oper_id, stored in
     * an alternating, flat list -- returns <code>null</code> instead if all chains are (implicitly) member of assembly
     * "1"
     */
    public static Map<String, List<String>> of(MmCifBlock block) {
        Set<String> candidates = getAssemblyCandidates(block);
        if (candidates == null) {
            // this implies that all chains are part of the assembly if no information or a single assembly is given
            return null;
        }

        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        Map<String, List<String>> assemblyInformation = new LinkedHashMap<>();
        for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
            String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
            // ignore assemblies not deemed biologically meaningful
            if (!candidates.contains(assemblyId)) {
                continue;
            }

            String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
            String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
            List<String> rowSpecificOperList = parseOperList(operExpression, asymIdList);

            List<String> assemblySpecificOperList = assemblyInformation.computeIfAbsent(assemblyId, e -> new ArrayList<>());
            assemblySpecificOperList.addAll(rowSpecificOperList);
        }
        return assemblyInformation;
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private static final Pattern LIST_PATTERN = Pattern.compile(",");
    private static List<String> parseOperList(String operExpression, String asymIdList) {
        List<String> operations = new ArrayList<>();
        List<String> chains = LIST_PATTERN.splitAsStream(asymIdList).collect(Collectors.toList());
        String[] split = OPERATION_PATTERN.split(operExpression);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    for (String chain : chains) {
                        operations.add(chain);
                        operations.add(id1 + "x" + id2);
                    }
                }
            }
        } else {
            for (String id : extractTransformationIds(operExpression)) {
                for (String chain : chains) {
                    operations.add(chain);
                    operations.add(id);
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

    private static final Set<String> ALLOWED_DETAILS = Set.of(
            "author_and_software_defined_assembly",
            "author_defined_assembly",
            "complete icosahedral assembly",
            "complete point assembly",
            "crystal asymmetric unit",
            "crystal asymmetric unit, crystal frame",
            "details",
            "helical asymmetric unit",
            "helical asymmetric unit, std helical frame",
            "icosahedral 23 hexamer",
            "icosahedral asymmetric unit",
            "icosahedral asymmetric unit, std point frame",
            "icosahedral pentamer",
            "pentasymmetron capsid unit",
            "point asymmetric unit",
            "point asymmetric unit, std point frame",
            "representative helical assembly",
            "software_defined_assembly",
            "trisymmetron capsid unit",
            "deposited_coordinates"
    );
    private static final Set<String> IGNORED_DETAILS = Set.of(
            "crystal asymmetric unit",
            "crystal asymmetric unit, crystal frame",
            "helical asymmetric unit",
            "helical asymmetric unit, std helical frame",
            "icosahedral 23 hexamer",
            "icosahedral asymmetric unit",
            "icosahedral asymmetric unit, std point frame",
            "icosahedral pentamer",
            "pentasymmetron capsid unit",
            "point asymmetric unit",
            "point asymmetric unit, std point frame",
            "trisymmetron capsid unit",
            "deposited_coordinates",
            "details"
    );
    /**
     * Filters for biologically relevant assemblies by checking against a dictionary of accepted phrases. E.g., for 1m4x
     * a handful of assemblies are defined but only the 1st (complete) assembly is relevant and shown on rcsb.org.
     * See https://rcsbpdb.atlassian.net/browse/RO-172 and https://github.com/rcsb/py-rcsb_utils_dictionary/blob/acd11adede68c16c582bd24cf5d6966adbbdc22b/rcsb/utils/dictionary/DictMethodAssemblyHelper.py#L38
     * for details.
     * @param block data container
     * @return all selected assembly identifiers
     */
    static Set<String> getAssemblyCandidates(MmCifBlock block) {
        PdbxStructAssembly pdbxStructAssembly = block.getPdbxStructAssembly();
        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        // no work to do for structures without (multiple) assemblies
        if (!pdbxStructAssembly.isDefined() || pdbxStructAssemblyGen.getRowCount() < 2) {
            // signals to take deposited coordinates at face value
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        for (int i = 0; i < pdbxStructAssembly.getRowCount(); i++) {
            String d = pdbxStructAssembly.getDetails().get(i);
            if (!ALLOWED_DETAILS.contains(d) || IGNORED_DETAILS.contains(d)) {
                continue;
            }

            candidates.add(pdbxStructAssembly.getId().get(i));
        }

        if (candidates.isEmpty() || (candidates.size() == 1 && candidates.iterator().next().equals("1"))) {
            return null;
        }

        return candidates;
    }
}
