package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.ModifiedResidueStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import static org.junit.jupiter.api.Assertions.*;

class ResidueTypeResolverTest {
    private ResidueTypeResolver noneResolver;
    private ResidueTypeResolver internalResolver;

    @BeforeEach
    public void setup() {
        StrucmotifConfig none = new StrucmotifConfig();
        none.setModifiedResidueStrategy(ModifiedResidueStrategy.NONE);
        this.noneResolver = new ResidueTypeResolverImpl(none);

        StrucmotifConfig internal = new StrucmotifConfig();
        internal.setModifiedResidueStrategy(ModifiedResidueStrategy.INTERNAL);
        this.internalResolver = new ResidueTypeResolverImpl(internal);
    }

    @Test
    public void whenReadingModifiedResidues_thenStrategyHonored() {
        String test1 = "CSO";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test1), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.CYSTEINE, internalResolver.selectResidueType(test1), "should report unknown amino acid with INTERNAL strategy");

        String test2 = "???";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test2), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_COMPONENT, internalResolver.selectResidueType(test2), "should report unknown component with INTERNAL strategy");

        String test3 = "ALA";
        assertEquals(ResidueType.ALANINE, noneResolver.selectResidueType(test3), "should report ALANINE with NONE strategy");
        assertEquals(ResidueType.ALANINE, internalResolver.selectResidueType(test3), "should report ALANINE with INTERNAL strategy");

        String test4 = "DAL";
        assertEquals(ResidueType.UNKNOWN_COMPONENT, noneResolver.selectResidueType(test4), "should report unknown component with NONE strategy");
        assertEquals(ResidueType.ALANINE, internalResolver.selectResidueType(test4), "should report ALANINE with INTERNAL strategy");

        String test5 = "UNK";
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, noneResolver.selectResidueType(test5), "should report unknown amino acid with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_AMINO_ACID, internalResolver.selectResidueType(test5), "should report unknown amino acid with INTERNAL strategy");

        String test6 = "N";
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, noneResolver.selectResidueType(test6), "should report unknown nucleotide with NONE strategy");
        assertEquals(ResidueType.UNKNOWN_NUCLEOTIDE, internalResolver.selectResidueType(test6), "should report unknown nucleotide with INTERNAL strategy");
    }
}