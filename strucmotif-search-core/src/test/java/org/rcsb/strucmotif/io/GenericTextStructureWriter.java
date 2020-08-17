package org.rcsb.strucmotif.io;

import org.rcsb.cif.CifBuilder;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.FloatColumnBuilder;
import org.rcsb.cif.model.IntColumnBuilder;
import org.rcsb.cif.model.StrColumnBuilder;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifBlockBuilder;
import org.rcsb.cif.schema.mm.MmCifCategoryBuilder;
import org.rcsb.cif.schema.mm.MmCifFileBuilder;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A generic structure writer implementation to compose text mmCIF files.
 */
public class GenericTextStructureWriter {
    public void write(Structure structure, Path outputPath) throws IOException {
        // write full structure
        MmCifCategoryBuilder.AtomSiteBuilder atomSiteBuilder = CifBuilder.enterFile(StandardSchemata.MMCIF)
                .enterBlock(structure.getStructureIdentifier().getPdbId())
                .enterAtomSite();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> groupPDBBuilder = atomSiteBuilder.enterGroupPDB();
        IntColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> idBuilder = atomSiteBuilder.enterId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> typeSymbolBuilder = atomSiteBuilder.enterTypeSymbol();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelAtomIdBuilder = atomSiteBuilder.enterLabelAtomId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelAltIdBuilder = atomSiteBuilder.enterLabelAltId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelCompIdBuilder = atomSiteBuilder.enterLabelCompId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelAsymIdBuilder = atomSiteBuilder.enterLabelAsymId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelEntityIdBuilder = atomSiteBuilder.enterLabelEntityId();
        IntColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> labelSeqIdBuilder = atomSiteBuilder.enterLabelSeqId();
        StrColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> pdbxPDBInsCodeBuilder = atomSiteBuilder.enterPdbxPDBInsCode();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnXBuilder = atomSiteBuilder.enterCartnX();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnYBuilder = atomSiteBuilder.enterCartnY();
        FloatColumnBuilder<MmCifCategoryBuilder.AtomSiteBuilder, MmCifBlockBuilder, MmCifFileBuilder> cartnZBuilder = atomSiteBuilder.enterCartnZ();

        int counter = 0;
        for (Chain chain : structure.getChains()) {
            String asymId = chain.getChainIdentifier().getLabelAsymId() + chain.getChainIdentifier().getAssemblyId();
            String entityId = String.valueOf(structure.getChains().indexOf(chain) + 1);
            for (Residue residue : chain.getResidues()) {
                String threeLetterCode = residue.getResidueIdentifier().getResidueType().getThreeLetterCode();
                int seqId = residue.getResidueIdentifier().getLabelSeqId();
                for (Atom atom : residue.getAtoms()) {
                    groupPDBBuilder.add("ATOM");
                    idBuilder.add(++counter);
                    typeSymbolBuilder.add(atom.getAtomIdentifier().getLabelAtomId().substring(0, 1));
                    labelAtomIdBuilder.add(atom.getAtomIdentifier().getLabelAtomId());
                    if (atom.getAtomIdentifier().isAlternativeLocation()) {
                        labelAltIdBuilder.add(atom.getAtomIdentifier().getLabelAltId());
                    } else {
                        labelAltIdBuilder.markNextNotPresent();
                    }
                    labelCompIdBuilder.add(threeLetterCode);
                    labelAsymIdBuilder.add(asymId);
                    labelEntityIdBuilder.add(entityId);
                    labelSeqIdBuilder.add(seqId);
                    pdbxPDBInsCodeBuilder.markNextNotPresent();
                    double[] coord = atom.getCoord();
                    cartnXBuilder.add(coord[0]);
                    cartnYBuilder.add(coord[1]);
                    cartnZBuilder.add(coord[2]);
                }
            }
        }

        CifFile cifFile = atomSiteBuilder.leaveCategory().leaveBlock().leaveFile();
        CifIO.writeText(cifFile, outputPath);
    }
}
