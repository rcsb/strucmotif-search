package org.rcsb.strucmotif.wip;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.ResidueTypeResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

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
        private final MmCifFile mmCifFile;

        // all relevant categories
        private final String structureIdentifier;
        private final AtomSite atomSite;
//        private final PdbxStructAssemblyGen pdbxStructAssemblyGen;
//        private final PdbxStructOperList pdbxStructOperList;

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
            this.mmCifFile = mmCifFile;

            MmCifBlock block = mmCifFile.getFirstBlock();
            this.structureIdentifier = block.getBlockHeader();

            this.atomSite = block.getAtomSite();
//            this.pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
//            this.pdbxStructOperList = block.getPdbxStructOperList();

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
                min values: [-9455, -9694, -9147] @ [4V4G, 4F46, 3SXO]
                max values: [27864, 22349, 30178] @ [1Q5C, 1Q55, 3HQV]
                 */
                // TODO need underflow/overflow check here?
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

            // track last seen residueIndex
            chainOffsets.add(residueIndex);
//            Map<String, Transformation> transformations = buildTransformations();
//            Map<String, String[]> assemblies = AssemblyInformation.of(mmCifFile);

            return new FileBackedStructure(structureIdentifier,
                    null,
                    null,
                    null,
                    null,
                    null,
                    convertStrings(chainIds),
                    convertOffsets(chainOffsets),
                    convertLabelSeqId(labelSeqIdCollapsed),
                    convertOffsets(residueOffsets),
                    convertResidueTypes(residueTypes),
                    labelAtomId,
                    x,
                    y,
                    z
//                    assemblies.keySet().toArray(String[]::new),
//                    assemblies.values().toArray(String[][]::new),
//                    transformations.keySet().toArray(String[]::new),
//                    transformations.values().toArray(Transformation[]::new)
            );
        }
    }
}

