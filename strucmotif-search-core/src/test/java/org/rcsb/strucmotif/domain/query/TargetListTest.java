package org.rcsb.strucmotif.domain.query;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TargetListTest {
    private static final Set<String> TEST_CASES = Set.of("1acj", "1MUW", "1eXr", "AF-Q8W3K0-F1", "ma-bak-cepc-0001");

    @Test
    public void whenHandlingPdbIds_thenArchiveEntriesRetained() {
        assertEquals(3, TEST_CASES.stream()
                .filter(SearchSpace.PDB)
                .count());
    }

    @Test
    public void whenHandlingModelIds_thenArchiveEntriesIgnored() {
        assertEquals(2, TEST_CASES.stream()
                .filter(SearchSpace.MODELS)
                .count());
    }

    @Test
    public void whenAllTargetList_thenNoOperation() {
        assertEquals(TEST_CASES.size(), TEST_CASES.stream()
                .filter(SearchSpace.ALL)
                .count());
    }
}