package org.rcsb.strucmotif.update;

import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.domain.structure.EntryIds;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How identifiers are normalized before they are compared to what's already indexed.
 */
class StrucmotifUpdateIdentifierTest {
    /**
     * The regression guard for the delta: the RCSB Search API returns legacy IDs while the index holds extended ones.
     * Without normalizing the request first, every entry looks new on every run and the whole archive is re-indexed.
     */
    @Test
    void testThatTheDeltaIsEmptyForAlreadyIndexedEntries() {
        Set<String> known = Set.of("pdb_00004hhb", "AF_AFA0A009IHW8F1");

        List<String> delta = Stream.of("4HHB", "AF_AFA0A009IHW8F1")
                .map(EntryIds::indexed)
                .filter(id -> !known.contains(id))
                .toList();

        assertTrue(delta.isEmpty(), "Expected no new entries but got " + delta);
    }

    @Test
    void testThatGenuinelyNewEntriesStillAppearInTheDelta() {
        Set<String> known = Set.of("pdb_00004hhb");

        List<String> delta = Stream.of("4HHB", "2HBS")
                .map(EntryIds::indexed)
                .filter(id -> !known.contains(id))
                .toList();

        assertEquals(List.of("pdb_00002hbs"), delta);
    }
}
