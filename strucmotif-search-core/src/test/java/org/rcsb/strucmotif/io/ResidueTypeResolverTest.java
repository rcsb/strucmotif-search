package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.AmbiguousMonomerStrategy;
import org.rcsb.strucmotif.config.ModifiedResidueStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import static org.junit.jupiter.api.Assertions.*;

class ResidueTypeResolverTest {
    private ResidueTypeResolver noneResolver;
    private ResidueTypeResolver noneButDResolver;
    private ResidueTypeResolver internalResolver;
    private ResidueTypeResolver internalButNoDResolver;
    private ResidueTypeResolver ambiguousUnknownResolver;
    private ResidueTypeResolver ambiguousTypeResolver;
    private ResidueTypeResolver ambiguousAcidResolver;
    private ResidueTypeResolver ambiguousAmideResolver;

    @BeforeEach
    public void setup() {
        StrucmotifConfig none = new StrucmotifConfig();
        none.setModifiedResidueStrategy(ModifiedResidueStrategy.NONE);
        none.setSupportDAminoAcids(false);
        this.noneResolver = new ResidueTypeResolverImpl(none);

        StrucmotifConfig noneButD = new StrucmotifConfig();
        noneButD.setModifiedResidueStrategy(ModifiedResidueStrategy.NONE);
        noneButD.setSupportDAminoAcids(true);
        this.noneButDResolver = new ResidueTypeResolverImpl(noneButD);

        StrucmotifConfig internal = new StrucmotifConfig();
        internal.setModifiedResidueStrategy(ModifiedResidueStrategy.INTERNAL);
        internal.setSupportDAminoAcids(true);
        this.internalResolver = new ResidueTypeResolverImpl(internal);

        StrucmotifConfig internalButNoD = new StrucmotifConfig();
        internalButNoD.setModifiedResidueStrategy(ModifiedResidueStrategy.INTERNAL);
        internalButNoD.setSupportDAminoAcids(false);
        this.internalButNoDResolver = new ResidueTypeResolverImpl(internalButNoD);

        StrucmotifConfig ambiguousUnknown = new StrucmotifConfig();
        ambiguousUnknown.setAmbiguousMonomerStrategy(AmbiguousMonomerStrategy.UNKNOWN_COMPONENT);
        this.ambiguousUnknownResolver = new ResidueTypeResolverImpl(ambiguousUnknown);

        StrucmotifConfig ambiguousType = new StrucmotifConfig();
        ambiguousType.setAmbiguousMonomerStrategy(AmbiguousMonomerStrategy.TYPE);
        this.ambiguousTypeResolver = new ResidueTypeResolverImpl(ambiguousType);

        StrucmotifConfig ambiguousAcid = new StrucmotifConfig();
        ambiguousAcid.setAmbiguousMonomerStrategy(AmbiguousMonomerStrategy.ACID);
        this.ambiguousAcidResolver = new ResidueTypeResolverImpl(ambiguousAcid);

        StrucmotifConfig ambiguousAmide = new StrucmotifConfig();
        ambiguousAmide.setAmbiguousMonomerStrategy(AmbiguousMonomerStrategy.AMIDE);
        this.ambiguousAmideResolver = new ResidueTypeResolverImpl(ambiguousAmide);
    }

    @Test
    void whenReadingModifiedResidues_thenStrategyHonored() {
        String test1 = "CSO";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test1), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneButDResolver.selectResidueType(test1), "should report unknown component with NONE_BUT_D strategy");
        assertEquals(ResidueType.CYSTEINE, internalResolver.selectResidueType(test1), "should report unknown amino acid with INTERNAL strategy");
        assertEquals(ResidueType.CYSTEINE, internalButNoDResolver.selectResidueType(test1), "should report unknown amino acid with INTERNAL_BUT_NO_D strategy");

        String test2 = "???";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test2), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneButDResolver.selectResidueType(test2), "should report unknown component with NONE_BUT_D strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, internalResolver.selectResidueType(test2), "should report unknown component with INTERNAL strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, internalButNoDResolver.selectResidueType(test2), "should report unknown component with INTERNAL_BUT_NO_D strategy");

        String test3 = "ALA";
        assertEquals(ResidueType.ALANINE, noneResolver.selectResidueType(test3), "should report ALANINE with NONE strategy");
        assertEquals(ResidueType.ALANINE, noneButDResolver.selectResidueType(test3), "should report ALANINE with NONE_BUT_D strategy");
        assertEquals(ResidueType.ALANINE, internalResolver.selectResidueType(test3), "should report ALANINE with INTERNAL strategy");
        assertEquals(ResidueType.ALANINE, internalButNoDResolver.selectResidueType(test3), "should report ALANINE with INTERNAL_BUT_NO_D strategy");

        String test4 = "DAL";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test4), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.ALANINE, noneButDResolver.selectResidueType(test4), "should report ALANINE with NONE_BUT_D strategy");
        assertEquals(ResidueType.ALANINE, internalResolver.selectResidueType(test4), "should report ALANINE with INTERNAL strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, internalButNoDResolver.selectResidueType(test4), "should report ALANINE with INTERNAL_BUT_NO_D strategy");

        String test5 = "UNK";
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, noneResolver.selectResidueType(test5), "should report unknown amino acid with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, noneButDResolver.selectResidueType(test5), "should report unknown amino acid with NONE_BUT_D strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, internalResolver.selectResidueType(test5), "should report unknown amino acid with INTERNAL strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, internalButNoDResolver.selectResidueType(test5), "should report unknown amino acid with INTERNAL_BUT_NO_D strategy");

        String test6 = "N";
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, noneResolver.selectResidueType(test6), "should report unknown nucleotide with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, noneButDResolver.selectResidueType(test6), "should report unknown nucleotide with NONE_BUT_D strategy");
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, internalResolver.selectResidueType(test6), "should report unknown nucleotide with INTERNAL strategy");
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, internalButNoDResolver.selectResidueType(test6), "should report unknown nucleotide with INTERNAL_BUT_NO_D strategy");
    }

    @Test
    void whenReadingAmbiguousResidues_thenStrategyHonored() {
        String test1 = "ASP";
        assertEquals(ResidueType.ASPARTIC_ACID, ambiguousUnknownResolver.selectResidueType(test1), "should report ASP independent of ambiguous strategy");
        assertEquals(ResidueType.ASPARTIC_ACID, ambiguousTypeResolver.selectResidueType(test1), "should report ASP independent of ambiguous strategy");
        assertEquals(ResidueType.ASPARTIC_ACID, ambiguousAcidResolver.selectResidueType(test1), "should report ASP independent of ambiguous strategy");
        assertEquals(ResidueType.ASPARTIC_ACID, ambiguousAmideResolver.selectResidueType(test1), "should report ASP independent of ambiguous strategy");

        String test2 = "ASX";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, ambiguousUnknownResolver.selectResidueType(test2), "should report unknown component with unknown strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousTypeResolver.selectResidueType(test2), "should report unknown amino acid with type strategy");
        assertEquals(ResidueType.ASPARTIC_ACID, ambiguousAcidResolver.selectResidueType(test2), "should report ASP with acid strategy");
        assertEquals(ResidueType.ASPARAGINE, ambiguousAmideResolver.selectResidueType(test2), "should report ASN with amide strategy");

        String test3 = "GLN";
        assertEquals(ResidueType.GLUTAMINE, ambiguousUnknownResolver.selectResidueType(test3), "should report GLN independent of ambiguous strategy");
        assertEquals(ResidueType.GLUTAMINE, ambiguousTypeResolver.selectResidueType(test3), "should report GLN independent of ambiguous strategy");
        assertEquals(ResidueType.GLUTAMINE, ambiguousAcidResolver.selectResidueType(test3), "should report GLN independent of ambiguous strategy");
        assertEquals(ResidueType.GLUTAMINE, ambiguousAmideResolver.selectResidueType(test3), "should report GLN independent of ambiguous strategy");

        String test4 = "GLX";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, ambiguousUnknownResolver.selectResidueType(test4), "should report unknown component with unknown strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousTypeResolver.selectResidueType(test4), "should report unknown amino acid with type strategy");
        assertEquals(ResidueType.GLUTAMIC_ACID, ambiguousAcidResolver.selectResidueType(test4), "should report GLU with acid strategy");
        assertEquals(ResidueType.GLUTAMINE, ambiguousAmideResolver.selectResidueType(test4), "should report GLN with amide strategy");

        String test5 = "UNK";
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousUnknownResolver.selectResidueType(test5), "should report unknown amino acid independent of ambiguous strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousTypeResolver.selectResidueType(test5), "should report unknown amino acid independent of ambiguous strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousAcidResolver.selectResidueType(test5), "should report unknown amino acid independent of ambiguous strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, ambiguousAmideResolver.selectResidueType(test5), "should report unknown amino acid independent of ambiguous strategy");
    }
}