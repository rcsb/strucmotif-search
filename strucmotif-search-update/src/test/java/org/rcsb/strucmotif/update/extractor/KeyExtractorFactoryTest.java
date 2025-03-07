package org.rcsb.strucmotif.update.extractor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyExtractorFactoryTest {
    @Test
    public void whenRequestingExtractor_thenCorrectOnesReturned() {
        KeyExtractor extractor1 = KeyExtractorFactory.getKeyExtractor("AF-Q76EI6-F1-model_v1.cif");
        assertInstanceOf(AlphaFoldKeyExtractor.class, extractor1);

        KeyExtractor extractor2 = KeyExtractorFactory.getKeyExtractor("/path/to/ma-9z55z.cif");
        assertInstanceOf(ModelArchiveKeyExtractor.class, extractor2);

        KeyExtractor extractor3 = KeyExtractorFactory.getKeyExtractor("/1acj.cif");
        assertInstanceOf(GenericKeyExtractor.class, extractor3);

        KeyExtractor extractor4 = KeyExtractorFactory.getKeyExtractor("/");
        assertInstanceOf(GenericKeyExtractor.class, extractor4);

        KeyExtractor extractor5 = KeyExtractorFactory.getKeyExtractor("");
        assertInstanceOf(GenericKeyExtractor.class, extractor5);
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