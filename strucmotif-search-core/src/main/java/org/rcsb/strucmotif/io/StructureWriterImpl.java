package org.rcsb.strucmotif.io;

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
import org.rcsb.cif.schema.mm.MaQaMetricLocal;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifBlockBuilder;
import org.rcsb.cif.schema.mm.MmCifCategoryBuilder;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.MmCifFileBuilder;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.config.ResidueQualityStrategy;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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
public class StructureWriterImpl implements StructureWriter {
    private static final Logger logger = LoggerFactory.getLogger(StructureWriterImpl.class);
    private final CifOptions options;
    private final ResidueQualityStrategy residueQualityStrategy;
    private final double residueQualityCutoff;

    /**
     * Construct a writer.
     */
    public StructureWriterImpl(MotifSearchConfig motifSearchConfig) {
        int precision = motifSearchConfig.getRenumberedCoordinatePrecision();
        boolean gzipped = motifSearchConfig.isRenumberedGzip();
        this.options = CifOptions.builder()
                .encodingStrategyHint("atom_site", "Cartn_x", "delta", precision)
                .encodingStrategyHint("atom_site", "Cartn_y", "delta", precision)
                .encodingStrategyHint("atom_site", "Cartn_z", "delta", precision)
                .gzip(gzipped)
                .build();
        this.residueQualityStrategy = motifSearchConfig.getResidueQualityStrategy();
        this.residueQualityCutoff = motifSearchConfig.getResidueQualityCutoff();
        if (residueQualityStrategy == ResidueQualityStrategy.NONE) {
            logger.info("All valid residues be indexed");
        } else {
            logger.info("Residues will be filtered by {} with a cutoff of {}", residueQualityStrategy, residueQualityCutoff);
        }
    }

    @Override
    public void write(MmCifFile source, Path destination) {
        MmCifBlock block = source.getFirstBlock();
        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
        AtomSite atomSite = block.getAtomSite();
        String pdbId = block.getBlockHeader().toUpperCase();

        MmCifBlockBuilder outputBuilder = CifBuilder.enterFile(StandardSchemata.MMCIF)
                .enterBlock(pdbId.toUpperCase());

        if (pdbxStructAssemblyGen.isDefined()) {
            outputBuilder.addCategory(pdbxStructAssemblyGen);
        }

        if (pdbxStructOperList.isDefined()) {
            outputBuilder.addCategory(pdbxStructOperList);
        }

        // ensure that all needed atoms are present to make this residue useful during indexing
        List<LabelSelection> validResidues = determineValidResidues(block);

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
        Set<String> currentlyAcceptedLabelAtomIds = new HashSet<>();

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
            String currentLabelAltId = atomSite.getLabelAltId().get(row);
            String currentLabelCompId = atomSite.getLabelCompId().get(row);

            // skip atoms that will be ambiguous during alignment
            ResidueType residueType = ResidueType.ofThreeLetterCode(currentLabelCompId);
            if (ambiguousAtom(residueType, currentLabelAtomId)) {
                continue;
            }

            // skip residues without CA or CB (or equivalent)
            if (!validResidues.contains(new LabelSelection(currentLabelAsymId, "1", currentLabelSeqId))) {
                continue;
            }

            // skip non-first alt-locs
            if (!currentLabelAltId.isEmpty() &&
                    // if label atom id matches the last one accepted (and component didnt change) we are in trouble
                    currentLabelAsymId.equals(lastAcceptedLabelAsymId) &&
                    currentLabelSeqId == lastAcceptedLabelSeqId &&
                    currentLabelAtomId.equals(lastAcceptedAtomLabelId)) {
                // ignore alt locs for now - keep only first alt loc (e.g. in 5chq, chain B, pos 122, alt locs: B/C)
                continue;
            }

            // make sure that only the first occurrence of an atom name within a residue is written
            if (currentLabelAsymId.equals(lastAcceptedLabelAsymId) && currentLabelSeqId == lastAcceptedLabelSeqId) {
                if (currentlyAcceptedLabelAtomIds.contains(currentLabelAtomId)) {
                    continue;
                }
            } else {
                currentlyAcceptedLabelAtomIds.clear();
            }
            currentlyAcceptedLabelAtomIds.add(currentLabelAtomId);

            // stuff to skip alt-locs
            lastAcceptedAtomLabelId = currentLabelAtomId;
            lastAcceptedLabelSeqId = currentLabelSeqId;
            lastAcceptedLabelAsymId = currentLabelAsymId;

            labelAtomId.add(currentLabelAtomId);
            labelCompId.add(currentLabelCompId);
            labelAsymId.add(currentLabelAsymId);
            labelSeqId.add(currentLabelSeqId);
            cartnX.add(atomSite.getCartnX().get(row));
            cartnY.add(atomSite.getCartnY().get(row));
            cartnZ.add(atomSite.getCartnZ().get(row));
        }
        atomSiteBuilder.leaveCategory();
        CifFile outputFile = outputBuilder.leaveBlock().leaveFile();

        // skip empty files
        if (outputFile.getBlocks().get(0).getCategory("atom_site").getRowCount() == 0) {
            return;
        }

        try {
            CifIO.writeBinary(outputFile, destination, options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<LabelSelection> determineValidResidues(MmCifBlock block) {
        AtomSite atomSite = block.getAtomSite();
        Map<LabelSelection, ResidueType> residueTypes = new HashMap<>();
        Map<LabelSelection, Set<String>> presentAtoms = new HashMap<>();
        for (int row = 0; row < atomSite.getRowCount(); row++) {
            if (atomSite.getLabelSeqId().getValueKind(row) != ValueKind.PRESENT) {
                continue;
            }

            String labelAsymId = atomSite.getLabelAsymId().get(row);
            int labelSeqId = atomSite.getLabelSeqId().get(row);

            // filter away residues with low confidence, if requested
            if (residueQualityStrategy != ResidueQualityStrategy.NONE) {
                switch (residueQualityStrategy) {
                    // filter by B-factor
                    case BFACTOR_ABOVE_CUTOFF: case BFACTOR_BELOW_CUTOFF:
                        if (atomSite.getBIsoOrEquiv().isDefined() && atomSite.getBIsoOrEquiv().getValueKind(row) == ValueKind.PRESENT) {
                            if (!residueQualityStrategy.test(atomSite.getBIsoOrEquiv().get(row), residueQualityCutoff)) {
                                continue;
                            }
                        }
                        // filter by qa-metric
                    case QA_METRIC_LOCAL_ABOVE_CUTOFF: case QA_METRIC_LOCAL_BELOW_CUTOFF:
                        MaQaMetricLocal category = block.getMaQaMetricLocal();
                        if (category.isDefined()) {
                            double metricValue = findMetricValue(category,labelAsymId, labelSeqId);
                            if (!residueQualityStrategy.test(metricValue, residueQualityCutoff)) {
                                continue;
                            }
                        }
                }
            }

            LabelSelection labelSelection = new LabelSelection(labelAsymId, "1", labelSeqId);
            residueTypes.put(labelSelection, ResidueType.ofThreeLetterCode(atomSite.getLabelCompId().get(row)));
            Set<String> atoms = presentAtoms.computeIfAbsent(labelSelection, e -> new HashSet<>());
            atoms.add(atomSite.getLabelAtomId().get(row));
        }
        List<LabelSelection> validResidues = new ArrayList<>();
        for (Map.Entry<LabelSelection, ResidueType> entry : residueTypes.entrySet()) {
            LabelSelection labelSelection = entry.getKey();
            ResidueType residueType = residueTypes.get(labelSelection);
            Set<String> atoms = presentAtoms.get(labelSelection);
            if (residueType.getPolymerType() == PolymerType.AMINO_ACID) {
                if (residueType != ResidueType.GLYCINE) {
                    if (atoms.contains("CA") && atoms.contains("CB")) {
                        validResidues.add(labelSelection);
                    }
                } else {
                    if (atoms.contains("N") && atoms.contains("CA") && atoms.contains("C")) {
                        validResidues.add(labelSelection);
                    }
                }
            } else {
                if (atoms.contains("C4'") && atoms.contains("C1'")) {
                    validResidues.add(labelSelection);
                }
            }
        }
        return validResidues;
    }

    /**
     * Determine if atoms of a certain name are useful to find the best alignment. Ambiguous atom names cause serious
     * problems. Strategy: Ignore them!
     * @param residueType reference residue type
     * @param labelAtomId the atom name to process
     * @return true if this atom is useless for RMSD calculations
     */
    private static boolean ambiguousAtom(ResidueType residueType, String labelAtomId) {
        // see Coutsias, 2019
        switch (residueType) {
            case ARGININE:
                if (labelAtomId.equals("NH1") || labelAtomId.equals("NH2")) {
                    return true;
                }
            case ASPARTIC_ACID:
                if (labelAtomId.equals("OD1") || labelAtomId.equals("OD2")) {
                    return true;
                }
            case GLUTAMIC_ACID:
                if (labelAtomId.equals("OE1") || labelAtomId.equals("OE2")) {
                    return true;
                }
            case LEUCINE:
                if (labelAtomId.equals("OD1") || labelAtomId.equals("OD2")) {
                    return true;
                }
            case PHENYLALANINE: case TYROSINE:
                if (labelAtomId.equals("CD1") || labelAtomId.equals("CD2") || labelAtomId.equals("CE1") ||
                        labelAtomId.equals("CE2")) {
                    return true;
                }
            case  VALINE:
                if (labelAtomId.equals("OG1") || labelAtomId.equals("OG2")) {
                    return true;
                }
            default:
                return false;
        }
    }

    private double findMetricValue(MaQaMetricLocal category, String labelAsymId, int labelSeqId) {
        for (int i = 0; i < category.getRowCount(); i++) {
            if (category.getLabelAsymId().get(i).equals(labelAsymId) && category.getLabelSeqId().get(i) == labelSeqId) {
                return category.getMetricValue().get(i);
            }
        }

        throw new NoSuchElementException("No in '" + category.getCategoryName() + "' for '" + labelAsymId + "-" + labelSeqId + "'");
    }
}
