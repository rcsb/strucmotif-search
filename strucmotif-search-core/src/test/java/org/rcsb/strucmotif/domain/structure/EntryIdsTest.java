package org.rcsb.strucmotif.domain.structure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryIdsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "pdb_00004hhb",
            "pdb_1000axyz" // natively 12-character, as issued after the cutover
    })
    void testThatExtendedIdentifiersAreRecognized(String value) {
        assertTrue(EntryIds.isExtendedPdbId(value));
        assertTrue(EntryIds.isPdbId(value));
        assertFalse(EntryIds.isComputedModelId(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "4HHB", "4hhb", "1abc", "1234" })
    void testThatLegacyIdentifiersAreRecognized(String value) {
        assertTrue(EntryIds.isLegacyPdbId(value));
        assertTrue(EntryIds.isPdbId(value));
        assertFalse(EntryIds.isComputedModelId(value));
    }

    /**
     * Everything that isn't a PDB ID is a computed structure model, so the shapes RCSB writes are covered without
     * enumerating them.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "AF_AFA0A009IHW8F1",
            "MA_MAASFVASFVG001",
            "af_afa0a009ihw8f1", // computed structure model IDs occur in either case
            "ma_bak_cepc_0001"
    })
    void testThatEverythingElseIsAComputedModel(String value) {
        assertTrue(EntryIds.isComputedModelId(value));
        assertFalse(EntryIds.isPdbId(value));
    }

    /**
     * Extended PDB IDs are lowercase, as normalized upstream. An upper-cased one isn't recognized as a PDB entry.
     */
    @Test
    void testThatUpperCaseExtendedIdentifiersAreNotPdbIds() {
        assertFalse(EntryIds.isExtendedPdbId("PDB_00004HHB"));
        assertFalse(EntryIds.isPdbId("PDB_00004HHB"));
    }

    @Test
    void testThatNullIsNeitherShape() {
        assertFalse(EntryIds.isExtendedPdbId(null));
        assertFalse(EntryIds.isLegacyPdbId(null));
        assertFalse(EntryIds.isPdbId(null));
        assertFalse(EntryIds.isComputedModelId(null));
    }

    @Test
    void testThatLegacyIdentifiersAreIndexedAsExtended() {
        assertEquals("pdb_00004hhb", EntryIds.indexed("4HHB"));
        assertEquals("pdb_00004hhb", EntryIds.indexed("4hhb"));
        assertEquals("pdb_00001abc", EntryIds.indexed("1ABC"));
    }

    @Test
    void testThatEverythingElseIsIndexedAsWritten() {
        assertEquals("pdb_00004hhb", EntryIds.indexed("pdb_00004hhb"));
        assertEquals("AF_AFA0A009IHW8F1", EntryIds.indexed("AF_AFA0A009IHW8F1"));
        assertEquals("https://example.com/4hhb.bcif", EntryIds.indexed("https://example.com/4hhb.bcif"));
        assertNull(EntryIds.indexed(null));
    }

    @Test
    void testThatTheLegacyCodeIsRecovered() {
        assertEquals("4hhb", EntryIds.legacyCode("pdb_00004hhb"));
        assertEquals("4hhb", EntryIds.legacyCode(EntryIds.indexed("4HHB")));
    }

    @Test
    void testThatIdentifiersWithoutALegacyFormHaveNoCode() {
        assertNull(EntryIds.legacyCode("pdb_1000axyz")); // issued after the cutover
        assertNull(EntryIds.legacyCode("pdb_0000axyz")); // legacy IDs start with a digit
        assertNull(EntryIds.legacyCode("4HHB"));
        assertNull(EntryIds.legacyCode("AF_AFA0A009IHW8F1"));
        assertNull(EntryIds.legacyCode(null));
    }

    /**
     * An extended ID must shard like the legacy ID it corresponds to, rather than putting the whole archive under 'db'.
     */
    @Test
    void testThatTheShardMatchesBetweenLegacyAndExtended() {
        assertEquals("HH", EntryIds.middle("4HHB"));
        assertEquals("hh", EntryIds.middle("pdb_00004hhb"));
        assertEquals("hh", EntryIds.middle(EntryIds.indexed("4HHB")));
        assertEquals("F_", EntryIds.middle("AF_AFA0A009IHW8F1"));
    }

    /**
     * Identifiers lose their case when round-tripped through a CIF block header, which ciftools upper-cases.
     */
    @Test
    void testThatCanonicalRestoresTheCaseOfExtendedIdentifiers() {
        assertEquals("pdb_00004hhb", EntryIds.canonical("PDB_00004HHB"));
        assertEquals("pdb_00004hhb", EntryIds.canonical("pdb_00004hhb"));
        assertEquals("pdb_00004hhb", EntryIds.canonical("Pdb_00004HhB"));
    }

    @Test
    void testThatCanonicalLeavesOtherIdentifiersAlone() {
        assertEquals("4HHB", EntryIds.canonical("4HHB"));
        assertEquals("AF_AFA0A009IHW8F1", EntryIds.canonical("AF_AFA0A009IHW8F1"));
        assertEquals("not-an-id", EntryIds.canonical("not-an-id"));
        assertNull(EntryIds.canonical(null));
    }
}
