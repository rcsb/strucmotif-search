package org.rcsb.strucmotif.update.extractor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyExtractorFactoryTest {
    @Test
    public void whenRequestingExtractor_thenCorrectOnesReturned() {
        KeyExtractor extractor1 = KeyExtractorFactory.getKeyExtractor("AF-Q76EI6-F1-model_v1.cif");
        assertTrue(extractor1 instanceof AlphaFoldKeyExtractor);

        KeyExtractor extractor2 = KeyExtractorFactory.getKeyExtractor("/path/to/ma-9z55z.cif");
        assertTrue(extractor2 instanceof ModelArchiveKeyExtractor);

        KeyExtractor extractor3 = KeyExtractorFactory.getKeyExtractor("/1acj.cif");
        assertTrue(extractor3 instanceof GenericKeyExtractor);

        KeyExtractor extractor4 = KeyExtractorFactory.getKeyExtractor("/");
        assertTrue(extractor4 instanceof GenericKeyExtractor);

        KeyExtractor extractor5 = KeyExtractorFactory.getKeyExtractor("");
        assertTrue(extractor5 instanceof GenericKeyExtractor);
    }

    @Test
    public void whenExtractingKeys_thenCorrectOnesReturned() {
        String key1 = KeyExtractorFactory.getKey("AF-Q76EI6-F1-model_v1.cif");
        assertEquals("AF-Q76EI6-F1", key1);

        String key2 = KeyExtractorFactory.getKey("/path/to/ma-9z55z.cif");
        assertEquals("MA-9Z55Z", key2);

        String key3 = KeyExtractorFactory.getKey("/1acj.cif");
        assertEquals("1ACJ", key3);
    }
}