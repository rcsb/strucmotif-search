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
import java.util.*;
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

            AssemblyInformation assemblyInformation = parseAssemblies(block, chainIds);
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
     * See <a href="https://rcsbpdb.atlassian.net/browse/RO-172">RO-172</a> and <a href="https://github.com/rcsb/py-rcsb_utils_dictionary/blob/acd11adede68c16c582bd24cf5d6966adbbdc22b/rcsb/utils/dictionary/DictMethodAssemblyHelper.py#L38">py-rcsb_utils_dictionary</a>
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

    record AssemblyInformation(String[] assemblyIdentifiers, int[] assemblyOffsets, String[] assemblyReferences, String[] transformationIdentifiers, float[] transformations) {}

    private static final String[] DEFAULT_ASSEMBLY_IDENTIFIERS = new String[] { "1" };
    private static final int[] DEFAULT_ASSEMBLY_OFFSETS = new int[] { 0 };
    private static final String[] DEFAULT_ASSEMBLY_REFERENCES = new String[] { "A", "1" };
    private static final String[] DEFAULT_TRANSFORMATION_IDENTIFIERS = new String[] { "1" };
    private static final float[] DEFAULT_TRANSFORMATION = new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 };
    private static final Map<String, float[]> DEFAULT_STRUCT_OPER_LIST = Map.of(DEFAULT_TRANSFORMATION_IDENTIFIERS[0], DEFAULT_TRANSFORMATION);
    private static final AssemblyInformation DEFAULT_ASSEMBLY_INFORMATION = new AssemblyInformation(DEFAULT_ASSEMBLY_IDENTIFIERS,
            DEFAULT_ASSEMBLY_OFFSETS, DEFAULT_ASSEMBLY_REFERENCES, DEFAULT_TRANSFORMATION_IDENTIFIERS, DEFAULT_TRANSFORMATION);

    /**
     * Construct assembly information instance from source file. Returns <code>null</code> for the trivial case of a
     * single assembly and no transforms.
     * @param block data container
     * @param chainIds relevant chains, operations on everything else will be ignored
     * @return a map, keys are assembly identifiers, values are associated label_asym_id and struct_oper_id, stored in
     * an alternating, flat list -- returns <code>null</code> instead if all chains are (implicitly) member of assembly
     * "1" and no transforms have been registered
     */
    AssemblyInformation parseAssemblies(MmCifBlock block, List<String> chainIds) {
        // filter away non-biological assemblies
        Set<String> assemblyCandidates = getAssemblyCandidates(block);
        if (assemblyCandidates.isEmpty()) {
            // no (valid) assembly information, could throw here, let's treat it as "1" with no transforms for now
            return defaultAssemblyInformation(chainIds);
        }

        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        Map<String, List<String>> assemblyInformation = new LinkedHashMap<>();
        Set<String> chainIdSet = new HashSet<>(chainIds);
        for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
            String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
            // ignore assemblies not deemed biologically meaningful
            if (!assemblyCandidates.contains(assemblyId)) {
                continue;
            }

            String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
            String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
            List<String> rowSpecificOperList = parseOperList(operExpression, asymIdList, chainIdSet);
            // doesn't reference any meaningful chain
            if (rowSpecificOperList.isEmpty()) {
                continue;
            }

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
            return defaultAssemblyInformation(chainIds);
        }

        // parse transformations
        Map<String, float[]> transformationMap = parseStructOperList(block);
        String[] transformationIdentifiers = transformationMap.keySet().toArray(String[]::new);
        float[] transformations = new float[transformationMap.size() * 16];
        int p = 0;
        for (float[] transformation : transformationMap.values()) {
            System.arraycopy(transformation, 0, transformations, p, transformation.length);
            p += 16;
        }

        return new AssemblyInformation(assemblyIdentifiers,
                assemblyOffsets,
                assemblyReferences,
                transformationIdentifiers,
                transformations);
    }

    private AssemblyInformation defaultAssemblyInformation(List<String> chainIds) {
        if (chainIds.size() == 1 && chainIds.contains("A")) {
            return DEFAULT_ASSEMBLY_INFORMATION;
        } else {
            String[] assemblyReferences = chainIds.stream()
                    .flatMap(c -> Stream.of(c, DEFAULT_TRANSFORMATION_IDENTIFIERS[0]))
                    .toArray(String[]::new);
            return new AssemblyInformation(DEFAULT_ASSEMBLY_IDENTIFIERS, DEFAULT_ASSEMBLY_OFFSETS,
                    assemblyReferences, DEFAULT_TRANSFORMATION_IDENTIFIERS, DEFAULT_TRANSFORMATION);
        }
    }

    /**
     * Construct transformations from registered operations.
     * @return all transformations, identified by their structOperIdentifier
     */
    private Map<String, float[]> parseStructOperList(MmCifBlock block) {
        PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
        if (pdbxStructOperList.getRowCount() < 2) {
            return DEFAULT_STRUCT_OPER_LIST;
        }

        Map<String, float[]> matrices = IntStream.range(0, pdbxStructOperList.getRowCount())
                .boxed()
                .collect(Collectors.toMap(row -> pdbxStructOperList.getId().get(row),
                        row -> new float[] {
                                (float) pdbxStructOperList.getMatrix11().get(row),
                                (float) pdbxStructOperList.getMatrix12().get(row),
                                (float) pdbxStructOperList.getMatrix13().get(row),
                                (float) pdbxStructOperList.getVector1().get(row),

                                (float) pdbxStructOperList.getMatrix21().get(row),
                                (float) pdbxStructOperList.getMatrix22().get(row),
                                (float) pdbxStructOperList.getMatrix23().get(row),
                                (float) pdbxStructOperList.getVector2().get(row),

                                (float) pdbxStructOperList.getMatrix31().get(row),
                                (float) pdbxStructOperList.getMatrix32().get(row),
                                (float) pdbxStructOperList.getMatrix33().get(row),
                                (float) pdbxStructOperList.getVector3().get(row),

                                0, 0, 0, 1
                        }, (x, y) -> y, LinkedHashMap::new));

        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        Map<String, float[]> transformations = new LinkedHashMap<>();
        for (int row = 0; row < pdbxStructAssemblyGen.getRowCount(); row++) {
            String operExpression = pdbxStructAssemblyGen.getOperExpression().get(row);
            transformations.putAll(getTransformations(matrices, operExpression));
        }
        return transformations;
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private static final Pattern LIST_PATTERN = Pattern.compile(",");
    private List<String> parseOperList(String operExpression, String asymIdList, Set<String> chainIds) {
        List<String> operations = new ArrayList<>();
        List<String> chains = LIST_PATTERN.splitAsStream(asymIdList).toList();
        String[] split = OPERATION_PATTERN.split(operExpression);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    for (String chain : chains) {
                        if (!chainIds.contains(chain)) {
                            continue;
                        }
                        operations.add(chain);
                        operations.add(id1 + "x" + id2);
                    }
                }
            }
        } else {
            for (String id : extractTransformationIds(operExpression)) {
                for (String chain : chains) {
                    if (!chainIds.contains(chain)) {
                        continue;
                    }
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

    private static final String STRUCT_OPER_ID_DELIMITER = "x";
    private Map<String, float[]> getTransformations(Map<String, float[]> transformations, String operations) {
        Map<String, float[]> composedTransformations = new LinkedHashMap<>();

        String[] split = OPERATION_PATTERN.split(operations);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    composedTransformations.put(id1 + STRUCT_OPER_ID_DELIMITER + id2, multiply(transformations.get(id1), transformations.get(id2)));
                }
            }
        } else {
            List<String> ids = extractTransformationIds(operations);
            for (String id : ids) {
                composedTransformations.put(id, transformations.get(id));
            }
        }

        return composedTransformations;
    }

    private float[] multiply(float[] a, float[] b) {
        float a00 = a[0], a01 = a[1], a02 = a[2], a03 = a[3],
        a10 = a[4], a11 = a[5], a12 = a[6], a13 = a[7],
        a20 = a[8], a21 = a[9], a22 = a[10], a23 = a[11],
        a30 = a[12], a31 = a[13], a32 = a[14], a33 = a[15];
        float[] out = new float[16];

        // Cache only the current line of the second matrix
        float b0 = b[0], b1 = b[1], b2 = b[2], b3 = b[3];
        out[0] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[1] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[2] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[3] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[4]; b1 = b[5]; b2 = b[6]; b3 = b[7];
        out[4] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[5] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[6] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[7] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[8]; b1 = b[9]; b2 = b[10]; b3 = b[11];
        out[8] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[9] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[10] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[11] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        b0 = b[12]; b1 = b[13]; b2 = b[14]; b3 = b[15];
        out[12] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30;
        out[13] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31;
        out[14] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32;
        out[15] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33;

        return out;
    }
}

