package org.rcsb.strucmotif.domain.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsContentTypeTest {
    @Test
    void testThatLegacyIdentifiersAreExperimental() {
        assertTrue(ResultsContentType.EXPERIMENTAL.test("4HHB"));
        assertFalse(ResultsContentType.COMPUTATIONAL.test("4HHB"));
    }

    /**
     * Extended IDs used to fall through to COMPUTATIONAL, because the test only recognized the
     * legacy 4-character shape.
     */
    @Test
    void testThatExtendedIdentifiersAreExperimental() {
        assertTrue(ResultsContentType.EXPERIMENTAL.test("pdb_00004hhb"));
        assertFalse(ResultsContentType.COMPUTATIONAL.test("pdb_00004hhb"));
    }

    @Test
    void testThatComputedModelsAreComputational() {
        assertTrue(ResultsContentType.COMPUTATIONAL.test("AF_AFA0A009IHW8F1"));
        assertFalse(ResultsContentType.EXPERIMENTAL.test("AF_AFA0A009IHW8F1"));
        assertTrue(ResultsContentType.COMPUTATIONAL.test("MA_MAASFVASFVG001"));
    }
}
