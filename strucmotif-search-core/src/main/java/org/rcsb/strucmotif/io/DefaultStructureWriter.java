package org.rcsb.strucmotif.io;

import org.rcsb.cif.CifBuilder;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.CifOptions;
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
import org.rcsb.cif.schema.mm.PdbxStructAssembly;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.config.ResidueQualityStrategy;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
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
public class DefaultStructureWriter implements StructureWriter {
    private static final Logger logger = LoggerFactory.getLogger(DefaultStructureWriter.class);
    private final CifOptions options;
    private final ResidueQualityStrategy residueQualityStrategy;
    private final double residueQualityCutoff;
    private final ResidueTypeResolver residueTypeResolver;

    /**
     * Construct a writer.
     * @param residueTypeResolver the resolver instance
     * @param strucmotifConfig the global config
     */
    public DefaultStructureWriter(ResidueTypeResolver residueTypeResolver, StrucmotifConfig strucmotifConfig) {
        this.options = CifOptions.builder()
                .encodingStrategyHint("atom_site", "Cartn_x", "delta", 1)
                .encodingStrategyHint("atom_site", "Cartn_y", "delta", 1)
                .encodingStrategyHint("atom_site", "Cartn_z", "delta", 1)
                .gzip(true)
                .build();
        this.residueQualityStrategy = strucmotifConfig.getResidueQualityStrategy();
        this.residueQualityCutoff = strucmotifConfig.getResidueQualityCutoff();
        if (residueQualityStrategy == ResidueQualityStrategy.NONE) {
            logger.info("All valid residues be indexed");
        } else {
            logger.info("Residues will be filtered by {} with a cutoff of {}", residueQualityStrategy, residueQualityCutoff);
        }
        this.residueTypeResolver = residueTypeResolver;
    }

    @Override
    public byte[] write(MmCifFile source) {
        MmCifBlock block = source.getFirstBlock();
        PdbxStructAssembly pdbxStructAssembly = block.getPdbxStructAssembly();
        PdbxStructAssemblyGen pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        PdbxStructOperList pdbxStructOperList = block.getPdbxStructOperList();
        AtomSite atomSite = block.getAtomSite();
        String pdbId = block.getBlockHeader().toUpperCase();

        MmCifBlockBuilder outputBuilder = CifBuilder.enterFile(StandardSchemata.MMCIF)
                .enterBlock(pdbId.toUpperCase());

        if (pdbxStructAssembly.isDefined() && pdbxStructAssemblyGen.isDefined() && pdbxStructOperList.isDefined()) {
            outputBuilder.addCategory(pdbxStructAssembly);
            outputBuilder.addCategory(pdbxStructAssemblyGen);
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
        LabelAtomId lastAcceptedAtomLabelId = LabelAtomId.UNKNOWN_ATOM;
        Set<LabelAtomId> currentlyAcceptedLabelAtomIds = EnumSet.noneOf(LabelAtomId.class);

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
            LabelAtomId currentLabelAtomId = LabelAtomId.ofLabelAtomId(atomSite.getLabelAtomId().get(row));
            String currentLabelAltId = atomSite.getLabelAltId().get(row);
            String currentLabelCompId = atomSite.getLabelCompId().get(row);

            if (currentLabelAtomId == LabelAtomId.UNKNOWN_ATOM) {
                continue;
            }

            // skip atoms that will be ambiguous during alignment
            ResidueType residueType = residueTypeResolver.selectResidueType(currentLabelCompId);
            if (ambiguousAtom(residueType, currentLabelAtomId)) {
                continue;
            }

            // skip residues without CA or CB (or equivalent)
            if (!validResidues.contains(new LabelSelection(currentLabelAsymId, Transformation.DEFAULT_OPERATOR, currentLabelSeqId))) {
                continue;
            }

            // skip non-first alt-locs
            if (!currentLabelAltId.isEmpty() &&
                    // if label atom id matches the last one accepted (and component didn't change) we are in trouble
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

            labelAtomId.add(currentLabelAtomId.getLabelAtomId());
            labelCompId.add(currentLabelCompId);
            labelAsymId.add(currentLabelAsymId);
            labelSeqId.add(currentLabelSeqId);
            cartnX.add(atomSite.getCartnX().get(row));
            cartnY.add(atomSite.getCartnY().get(row));
            cartnZ.add(atomSite.getCartnZ().get(row));
        }
        atomSiteBuilder.leaveCategory();
        MmCifFile outputFile = outputBuilder.leaveBlock().leaveFile();

        // skip empty files
        if (outputFile.getBlocks().get(0).getAtomSite().getRowCount() == 0) {
            return null;
        }

        try {
            return CifIO.writeBinary(outputFile, options);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<LabelSelection> determineValidResidues(MmCifBlock block) {
        AtomSite atomSite = block.getAtomSite();
        Map<LabelSelection, ResidueType> residueTypes = new HashMap<>();
        Map<LabelSelection, Set<LabelAtomId>> presentAtoms = new HashMap<>();
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
                    default:
                        // everything else is allowed to pass
                }
            }

            LabelSelection labelSelection = new LabelSelection(labelAsymId, Transformation.DEFAULT_OPERATOR, labelSeqId);
            ResidueType residueType = residueTypeResolver.selectResidueType(atomSite.getLabelCompId().get(row));
            // don't write/index any unknown components (some will still pass this check due to microheterogenity like B-2 in 1aw8)
            if (residueType == ResidueType.UNKNOWN_COMPONENT) {
                continue;
            }

            residueTypes.put(labelSelection, residueType);
            Set<LabelAtomId> atoms = presentAtoms.computeIfAbsent(labelSelection, e -> EnumSet.noneOf(LabelAtomId.class));
            atoms.add(LabelAtomId.ofLabelAtomId(atomSite.getLabelAtomId().get(row)));
        }
        List<LabelSelection> validResidues = new ArrayList<>();
        for (Map.Entry<LabelSelection, ResidueType> entry : residueTypes.entrySet()) {
            LabelSelection labelSelection = entry.getKey();
            ResidueType residueType = entry.getValue();
            Set<LabelAtomId> atoms = presentAtoms.get(labelSelection);
            if (residueType.getPolymerType() == PolymerType.AMINO_ACID) {
                if (residueType != ResidueType.GLYCINE) {
                    if (atoms.contains(LabelAtomId.CA) && atoms.contains(LabelAtomId.CB)) {
                        validResidues.add(labelSelection);
                    }
                } else {
                    if (atoms.contains(LabelAtomId.N) && atoms.contains(LabelAtomId.CA) && atoms.contains(LabelAtomId.C)) {
                        validResidues.add(labelSelection);
                    }
                }
            } else {
                if (atoms.contains(LabelAtomId.C4_PRIME) && atoms.contains(LabelAtomId.C1_PRIME)) {
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
    private static boolean ambiguousAtom(ResidueType residueType, LabelAtomId labelAtomId) {
        // see Coutsias, 2019
        return switch (residueType) {
            case ARGININE -> (labelAtomId == LabelAtomId.NH1 || labelAtomId == LabelAtomId.NH2);
            case ASPARTIC_ACID -> (labelAtomId == LabelAtomId.OD1 || labelAtomId == LabelAtomId.OD2);
            case GLUTAMIC_ACID -> (labelAtomId == LabelAtomId.OE1 || labelAtomId == LabelAtomId.OE2);
            case PHENYLALANINE, TYROSINE -> (labelAtomId == LabelAtomId.CD1 || labelAtomId == LabelAtomId.CD2
                    || labelAtomId == LabelAtomId.CE1 || labelAtomId == LabelAtomId.CE2);
            default -> false;
        };
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
