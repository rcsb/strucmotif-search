package org.rcsb.strucmotif2.io.write;

import org.rcsb.cif.CifBuilder;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.CifOptions;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.FloatColumnBuilder;
import org.rcsb.cif.model.IntColumnBuilder;
import org.rcsb.cif.model.StrColumnBuilder;
import org.rcsb.cif.model.ValueKind;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifBlockBuilder;
import org.rcsb.cif.schema.mm.MmCifCategoryBuilder;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.MmCifFileBuilder;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.cif.schema.mm.Struct;
import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * This implementation keeps a sparse representation of structure data including:
 * <ul>
 *     <li>label_asym_id</li>
 *     <li>label_seq_id</li>
 *     <li>label_comp_id</li>
 *     <li>label_atom_id</li>
 *     <li>coordinates</li>
 *     <li>entry identifier and data used for assembly generation</li>
 * </ul>
 * <p>This implementation ignores:
 * <ul>
 *     <li>models with number unequal to 1</li>
 *     <li>hydrogen atoms</li>
 *     <li>non-polymer groups</li>
 *     <li>2nd and all subsequent alt locs</li>
 * </ul>
 */
@Service
public class RenumberedStructureWriterImpl implements RenumberedStructureWriter {
    private final CifOptions options;

    /**
     * Construct a writer.
     * @param motifSearchConfig the config
     */
    public RenumberedStructureWriterImpl(MotifSearchConfig motifSearchConfig) {
        int precision = motifSearchConfig.getRenumberedCoordinatePrecision();
        this.options = CifOptions.builder()
                .encodingStrategyHint("atom_site", "Cartn_x", "delta", precision)
                .encodingStrategyHint("atom_site", "Cartn_y", "delta", precision)
                .encodingStrategyHint("atom_site", "Cartn_z", "delta", precision)
                .gzip(true)
                .build();
    }

    @Override
    public void write(MmCifFile source, Path destination) {
        MmCifBlock block = source.getFirstBlock();
        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
        Struct struct = block.getStruct();
        AtomSite atomSite = block.getAtomSite();
        String pdbId = block.getBlockHeader().toLowerCase();

        MmCifBlockBuilder outputBuilder = CifBuilder.enterFile(StandardSchemata.MMCIF)
                .enterBlock(pdbId.toUpperCase());

        if (pdbxStructAssemblyGen.isDefined()) {
            outputBuilder.addCategory(pdbxStructAssemblyGen);
        }

        if (pdbxStructOperList.isDefined()) {
            outputBuilder.addCategory(pdbxStructOperList);
        }

        if (struct.isDefined() && struct.getTitle().isDefined()) {
            outputBuilder.enterStruct()
                    .enterTitle()
                    .add(struct.getTitle().get(0))
                    .leaveColumn()
                    .leaveCategory();
        }

        MmCifCategoryBuilder.AtomSiteBuilder atomSiteBuilder = outputBuilder.enterAtomSite();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelAtomId = atomSiteBuilder.enterLabelAtomId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelCompId = atomSiteBuilder.enterLabelCompId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelAsymId = atomSiteBuilder.enterLabelAsymId();
        IntColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelSeqId = atomSiteBuilder.enterLabelSeqId();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnX = atomSiteBuilder.enterCartnX();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnY = atomSiteBuilder.enterCartnY();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnZ = atomSiteBuilder.enterCartnZ();

        // keep track of alt locs
        String lastAcceptedLabelAsymId = "";
        int lastAcceptedLabelSeqId = Integer.MIN_VALUE;
        String lastAcceptedAtomLabelId = "";

        for (int row = 0; row < atomSite.getRowCount(); row++) {
            // ignore all models but the 1st
            if (atomSite.getPdbxPDBModelNum().isDefined() && atomSite.getPdbxPDBModelNum().get(row) != 1) {
                continue;
            }

            // skip hydrogen atoms
            String element = atomSite.getTypeSymbol().get(row);
            if ("H".equals(element) || "D".equals(element) || "T".equals(element)) {
                continue;
            }

            // skip non-polymer
            if (atomSite.getLabelSeqId().getValueKind(row) != ValueKind.PRESENT) {
                continue;
            }

            String currentLabelAsymId = atomSite.getLabelAsymId().get(row);
            int currentLabelSeqId = atomSite.getLabelSeqId().get(row);
            String currentLabelAtomId = atomSite.getLabelAtomId().get(row);
            String labelAltId = atomSite.getLabelAltId().get(row);
            // skip non-first alt-locs
            if (!labelAltId.isEmpty() &&
                    // if label atom id matches the last one accepted (and component didnt change) we are in trouble
                    currentLabelAsymId.equals(lastAcceptedLabelAsymId) &&
                    currentLabelSeqId == lastAcceptedLabelSeqId &&
                    currentLabelAtomId.equals(lastAcceptedAtomLabelId)) {
                // ignore alt locs for now - keep only first alt loc (e.g. in 5chq, chain B, pos 122, alt locs: B/C)
                continue;
            }

            // stuff to skip alt-locs
            lastAcceptedAtomLabelId = currentLabelAtomId;
            lastAcceptedLabelSeqId = currentLabelSeqId;
            lastAcceptedLabelAsymId = currentLabelAsymId;

            labelAtomId.add(currentLabelAtomId);
            labelCompId.add(atomSite.getLabelCompId().get(row));
            labelAsymId.add(currentLabelAsymId);
            labelSeqId.add(currentLabelSeqId);
            cartnX.add(atomSite.getCartnX().get(row));
            cartnY.add(atomSite.getCartnY().get(row));
            cartnZ.add(atomSite.getCartnZ().get(row));
        }
        atomSiteBuilder.leaveCategory();
        CifFile outputFile = outputBuilder.leaveBlock().leaveFile();

        try {
            CifIO.writeBinary(outputFile, destination, options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
