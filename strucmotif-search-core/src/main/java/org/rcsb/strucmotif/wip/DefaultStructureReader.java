package org.rcsb.strucmotif.wip;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxStructAssembly;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.ResidueTypeResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A simple structure reader that parses CIF data into structure instances.
 */
public class DefaultStructureReader {
    private final ResidueTypeResolver residueTypeResolver;

    /**
     * Create an instance.
     * @param residueTypeResolver the resolver instance
     */
    public DefaultStructureReader(ResidueTypeResolver residueTypeResolver) {
        this.residueTypeResolver = residueTypeResolver;
    }

    public Structure readFromInputStream(InputStream inputStream) {
        try {
            MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);
            return new DefaultStructureReader.StructureReaderState(mmCifFile).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    class StructureReaderState {
        // all relevant categories
        private final String structureIdentifier;
        private final MmCifBlock block;
        private final AtomSite atomSite;

        private final byte[] labelAtomId;
        private final String[] labelCompId;
        private final String[] labelAsymId;
        private final int[] labelSeqId;
        private final short[] x;
        private final short[] y;
        private final short[] z;

        // the 'state'
        private String lastLabelAsymId;
        private int lastLabelSeqId;
        private final List<String> chainIds;
        private final List<Integer> chainOffsets;
        private final List<Integer> labelSeqIdCollapsed;
        private final List<Integer> residueOffsets;
        private final List<ResidueType> residueTypes;

        /**
         * Initialize a new reading operation.
         * @param mmCifFile data source
         */
        private StructureReaderState(MmCifFile mmCifFile) {
            this.block = mmCifFile.getFirstBlock();
            this.atomSite = block.getAtomSite();

            this.structureIdentifier = block.getBlockHeader();

            this.labelAtomId = convertLabelAtomId(atomSite.getLabelAtomId().getArray());
            this.labelCompId = atomSite.getLabelCompId().getArray();
            this.labelAsymId = atomSite.getLabelAsymId().getArray();
            this.labelSeqId = atomSite.getLabelSeqId().getArray();
            this.x = convertCoords(atomSite.getCartnX().getArray());
            this.y = convertCoords(atomSite.getCartnY().getArray());
            this.z = convertCoords(atomSite.getCartnZ().getArray());

            this.lastLabelAsymId = null;
            this.lastLabelSeqId = -1;
            this.chainIds = new ArrayList<>();
            this.chainOffsets = new ArrayList<>();
            this.labelSeqIdCollapsed = new ArrayList<>();
            this.residueOffsets = new ArrayList<>();
            this.residueTypes = new ArrayList<>();
        }

        private short[] convertCoords(double[] array) {
            short[] out = new short[array.length];
            for (int i = 0; i < out.length; i++) {
                /*
                TODO need underflow/overflow check here?
                min values: [-9455, -9694, -9147] @ [4V4G, 4F46, 3SXO]
                max values: [27864, 22349, 30178] @ [1Q5C, 1Q55, 3HQV]
                 */
                out[i] = (short) Math.round(array[i] * 10);
            }
            return out;
        }

        private byte[] convertLabelAtomId(String[] array) {
            byte[] out = new byte[array.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) LabelAtomId.ofLabelAtomId(array[i]).ordinal();
            }
            return out;
        }

        private byte[] convertResidueTypes(List<ResidueType> residueTypes) {
            byte[] out = new byte[residueTypes.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) residueTypes.get(i).ordinal();
            }
            return out;
        }

        private int[] convertOffsets(List<Integer> offsets) {
            int[] out = new int[offsets.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = offsets.get(i);
            }
            return out;
        }

        private short[] convertLabelSeqId(List<Integer> labelSeqIdCollapsed) {
            short[] out = new short[labelSeqIdCollapsed.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = labelSeqIdCollapsed.get(i).shortValue();
            }
            return out;
        }

        private String[] convertStrings(List<String> values) {
            String[] out = new String[values.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = values.get(i);
            }
            return out;
        }

        private Structure build() {
            int residueIndex = 0;
            for (int row = 0; row < atomSite.getRowCount(); row++) {
                String labelAsymId = this.labelAsymId[row];
                int labelSeqId = this.labelSeqId[row];
                boolean chainChange = !labelAsymId.equals(lastLabelAsymId);
                boolean residueChange = labelSeqId != lastLabelSeqId;

                if (chainChange) {
                    chainIds.add(labelAsymId);
                    chainOffsets.add(residueIndex);
                }

                if (chainChange || residueChange) {
                    lastLabelAsymId = labelAsymId;
                    lastLabelSeqId = labelSeqId;
                    labelSeqIdCollapsed.add(labelSeqId);
                    residueOffsets.add(row);
                    residueTypes.add(residueTypeResolver.selectResidueType(labelCompId[row]));
                    residueIndex++;
                }
            }

            AssemblyInformation assemblyInformation = parseAssemblies(block);
            if (assemblyInformation == null) {
                // single assemblies
                return new FileBackedStructure(
                        structureIdentifier,
                        convertStrings(chainIds),
                        convertOffsets(chainOffsets),
                        convertLabelSeqId(labelSeqIdCollapsed),
                        convertOffsets(residueOffsets),
                        convertResidueTypes(residueTypes),
                        labelAtomId,
                        x, y, z
                );
            } else {
                return new FileBackedStructure(
                        structureIdentifier,
                        assemblyInformation.assemblyIdentifiers,
                        assemblyInformation.assemblyOffsets,
                        assemblyInformation.assemblyReferences,
                        assemblyInformation.transformationIdentifiers,
                        assemblyInformation.transformations,
                        convertStrings(chainIds),
                        convertOffsets(chainOffsets),
                        convertLabelSeqId(labelSeqIdCollapsed),
                        convertOffsets(residueOffsets),
                        convertResidueTypes(residueTypes),
                        labelAtomId,
                        x, y, z
                );
            }
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
    Set<String> getAssemblyCandidates(MmCifBlock block) {
        PdbxStructAssembly pdbxStructAssembly = block.getPdbxStructAssembly();

        Set<String> candidates = new LinkedHashSet<>();
        for (int i = 0; i < pdbxStructAssembly.getRowCount(); i++) {
            String d = pdbxStructAssembly.getDetails().get(i);
            if (!ALLOWED_DETAILS.contains(d) || IGNORED_DETAILS.contains(d)) {
                continue;
            }

            candidates.add(pdbxStructAssembly.getId().get(i));
        }

        return candidates;
    }

    record AssemblyInformation(String[] assemblyIdentifiers,
                               int[] assemblyOffsets,
                               String[] assemblyReferences,
                               String[] transformationIdentifiers,
                               float[] transformations) {}

    /**
     * Construct assembly information instance from source file. Returns <code>null</code> for the trivial case of a
     * single assembly and no transforms.
     * @param block data container
     * @return a map, keys are assembly identifiers, values are associated label_asym_id and struct_oper_id, stored in
     * an alternating, flat list -- returns <code>null</code> instead if all chains are (implicitly) member of assembly
     * "1" and no transforms have been registered
     */
    AssemblyInformation parseAssemblies(MmCifBlock block) {
        // filter away non-biological assemblies
        Set<String> assemblyCandidates = getAssemblyCandidates(block);
        if (assemblyCandidates.isEmpty()) {
            // no (valid) assembly information, could throw here, let's treat it as "1" with no transforms for now
            return null;
        }

        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        Map<String, List<String>> assemblyInformation = new LinkedHashMap<>();
        for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
            String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
            // ignore assemblies not deemed biologically meaningful
            if (!assemblyCandidates.contains(assemblyId)) {
                continue;
            }

            String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
            String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
            List<String> rowSpecificOperList = parseOperList(operExpression, asymIdList);

            List<String> assemblySpecificOperList = assemblyInformation.computeIfAbsent(assemblyId, e -> new ArrayList<>());
            assemblySpecificOperList.addAll(rowSpecificOperList);
        }

        // collect chain references
        String[] assemblyIdentifiers = assemblyInformation.keySet().toArray(String[]::new);
        int[] assemblyOffsets = new int[assemblyIdentifiers.length];
        String[] assemblyReferences = new String[assemblyInformation.values().stream().mapToInt(Collection::size).sum()];
        int offset = 0;
        int refPos = 0;
        for (int i = 0; i < assemblyIdentifiers.length; i++) {
            assemblyOffsets[i] = offset;
            List<String> partial = assemblyInformation.get(assemblyIdentifiers[i]);
            offset += partial.size();

            for (String p : partial) {
                assemblyReferences[refPos++] = p;
            }
        }

        PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
        if (assemblyIdentifiers.length == 1 && pdbxStructOperList.getRowCount() == 1) {
            // don't bother holding uninteresting information
            return null;
        }

        // parse transformations
        String[] transformationIdentifiers = null;
        float[] transformations = null;

        return new AssemblyInformation(assemblyIdentifiers, assemblyOffsets, assemblyReferences, transformationIdentifiers, transformations);
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private static final Pattern LIST_PATTERN = Pattern.compile(",");
    private List<String> parseOperList(String operExpression, String asymIdList) {
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
    private List<String> extractTransformationIds(String rawOperation) {
        String prepared = rawOperation.replace("(", "")
                .replace(")", "")
                .replace("'", "");

        return COMMA_PATTERN.splitAsStream(prepared)
                .flatMap(this::extractTransformationRanges)
                .collect(Collectors.toList());
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("-");
    private Stream<String> extractTransformationRanges(String raw) {
        String[] s = RANGE_PATTERN.split(raw);
        if (s.length == 1) {
            return Stream.of(raw);
        } else {
            return IntStream.range(Integer.parseInt(s[0]), Integer.parseInt(s[1]) + 1)
                    .mapToObj(String::valueOf);
        }
    }
}

