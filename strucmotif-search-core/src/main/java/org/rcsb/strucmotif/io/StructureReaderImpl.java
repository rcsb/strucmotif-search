package org.rcsb.strucmotif.io;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.math.Algebra;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A simple structure reader that parses CIF data into structure instances.
 */
@Service
public class StructureReaderImpl implements StructureReader {
    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        try {
            MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);
            return new StructureReaderState(mmCifFile).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class StructureReaderState {
        // all relevant categories
        private final String structureIdentifier;
        private final AtomSite atomSite;
        private final PdbxStructAssemblyGen pdbxStructAssemblyGen;
        private final PdbxStructOperList pdbxStructOperList;

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
        private final Map<String, int[]> chainOffsets;
        private final List<Integer> labelSeqIdCollapsed;
        private final List<Integer> residueOffsets;
        private final List<ResidueType> residueTypes;

        /**
         * Initialize a new reading operation.
         * @param mmCifFile data source in binary format
         */
        private StructureReaderState(MmCifFile mmCifFile) {
            MmCifBlock block = mmCifFile.getFirstBlock();
            this.structureIdentifier = block.getBlockHeader();

            this.atomSite = block.getAtomSite();
            this.pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
            this.pdbxStructOperList = block.getPdbxStructOperList();

            this.labelAtomId = convertLabelAtomId(atomSite.getLabelAtomId().getArray());
            this.labelCompId = atomSite.getLabelCompId().getArray();
            this.labelAsymId = atomSite.getLabelAsymId().getArray();
            this.labelSeqId = atomSite.getLabelSeqId().getArray();
            this.x = convertCoords(atomSite.getCartnX().getArray());
            this.y = convertCoords(atomSite.getCartnY().getArray());
            this.z = convertCoords(atomSite.getCartnZ().getArray());

            this.lastLabelAsymId = null;
            this.lastLabelSeqId = -1;
            this.chainOffsets = new LinkedHashMap<>();
            this.labelSeqIdCollapsed = new ArrayList<>();
            this.residueOffsets = new ArrayList<>();
            this.residueTypes = new ArrayList<>();
        }

        private short[] convertCoords(double[] array) {
            short[] out = new short[array.length];
            for (int i = 0; i < out.length; i++) {
                // TODO need underflow/overflow check here? 3j3q e.g. is in range of [0.0, 1100.0] and more than safe
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

        private Structure build() {
            int residueIndex = 0;
            for (int row = 0; row < atomSite.getRowCount(); row++) {
                String labelAsymId = this.labelAsymId[row];
                int labelSeqId = this.labelSeqId[row];
                boolean chainChange = !labelAsymId.equals(lastLabelAsymId);
                boolean residueChange = labelSeqId != lastLabelSeqId;

                if (chainChange) {
                    chainOffsets.put(labelAsymId, new int[] { residueIndex, residueIndex });
                }

                if (chainChange || residueChange) {
                    lastLabelAsymId = labelAsymId;
                    lastLabelSeqId = labelSeqId;
                    chainOffsets.get(labelAsymId)[1] = residueIndex;
                    labelSeqIdCollapsed.add(labelSeqId);
                    residueOffsets.add(row);
                    residueTypes.add(ResidueType.ofThreeLetterCode(labelCompId[row]));
                    residueIndex++;
                }
            }

            Map<String, Transformation> transformations = buildTransformations();
            Map<String, List<String>> assemblies = buildAssemblies();
            return new Structure(structureIdentifier,
                    chainOffsets,
                    convertOffsets(labelSeqIdCollapsed),
                    convertOffsets(residueOffsets),
                    convertResidueTypes(residueTypes),
                    labelAtomId,
                    x, y, z,
                    assemblies, transformations);
        }

        private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
        private Map<String, Transformation> getTransformations(Map<String, float[][]> transformations, String operations) {
            Map<String, Transformation> composedTransformations = new HashMap<>();

            String[] split = OPERATION_PATTERN.split(operations);
            if (split.length > 1) {
                List<String> ids1 = extractTransformationIds(split[0]);
                List<String> ids2 = extractTransformationIds(split[1]);
                for (String id1 : ids1) {
                    for (String id2 : ids2) {
                        composedTransformations.put(id1 + "x" + id2, Transformation.of(Algebra.multiply4d(transformations.get(id1), transformations.get(id2))));
                    }
                }
            } else {
                List<String> ids = extractTransformationIds(operations);
                for (String id : ids) {
                    composedTransformations.put(id, Transformation.of(transformations.get(id)));
                }
            }

            return composedTransformations;
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

        /**
         * Construct transformations from registered operations.
         * @return all transformations, identified by their structOperIdentifier
         */
        private Map<String, Transformation> buildTransformations() {
            Map<String, Transformation> transformations = new HashMap<>();
            Map<String, float[][]> matrices = IntStream.range(0, pdbxStructOperList.getRowCount())
                    .boxed()
                    .collect(Collectors.toMap(row -> pdbxStructOperList.getId().get(row),
                            row -> new float[][] {
                                    { (float) pdbxStructOperList.getMatrix11().get(row), (float) pdbxStructOperList.getMatrix12().get(row),
                                            (float) pdbxStructOperList.getMatrix13().get(row), (float) pdbxStructOperList.getVector1().get(row) },
                                    { (float) pdbxStructOperList.getMatrix21().get(row), (float) pdbxStructOperList.getMatrix22().get(row),
                                            (float) pdbxStructOperList.getMatrix23().get(row), (float) pdbxStructOperList.getVector2().get(row) },
                                    { (float) pdbxStructOperList.getMatrix31().get(row), (float) pdbxStructOperList.getMatrix32().get(row),
                                            (float) pdbxStructOperList.getMatrix33().get(row), (float) pdbxStructOperList.getVector3().get(row) },
                                    { 0, 0, 0, 1 }
                            }));

            if (pdbxStructAssemblyGen.isDefined()) {
                for (int row = 0; row < pdbxStructAssemblyGen.getRowCount(); row++) {
                    String operExpression = pdbxStructAssemblyGen.getOperExpression().get(row);
                    transformations.putAll(getTransformations(matrices, operExpression));
                }
            } else {
                // nothing defined explicitly
                transformations.put("1", Transformation.IDENTITY_TRANSFORMATION);
            }

            return transformations;
        }

        private Map<String, List<String>> buildAssemblies() {
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
            Map<String, List<String>> assemblyInformation = new LinkedHashMap<>();
            if (pdbxStructAssemblyGen.isDefined()) {
                for (int i = 0; i < pdbxStructAssemblyGen.getRowCount(); i++) {
                    String assemblyId = pdbxStructAssemblyGen.getAssemblyId().get(i);
                    String operExpression = pdbxStructAssemblyGen.getOperExpression().get(i);
                    String asymIdList = pdbxStructAssemblyGen.getAsymIdList().get(i);
                    List<String> operList = getOperList(operExpression, asymIdList);

                    List<String> chains = assemblyInformation.computeIfAbsent(assemblyId, e -> new ArrayList<>());
                    chains.addAll(operList);
                }
            }
            return assemblyInformation;
        }

        private static final Pattern LIST_PATTERN = Pattern.compile(",");
        private List<String> getOperList(String operExpression, String asymIdList) {
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
    }
}
