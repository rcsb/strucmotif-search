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

        KeyExtractor extractor3 = KeyExtractorFactory.getKeyExtractor("1acj.cif");
        assertTrue(extractor3 instanceof GenericKeyExtractor);

        KeyExtractor extractor4 = KeyExtractorFactory.getKeyExtractor("/");
        assertTrue(extractor4 instanceof GenericKeyExtractor);

        KeyExtractor extractor5 = KeyExtractorFactory.getKeyExtractor("");
        assertTrue(extractor5 instanceof GenericKeyExtractor);
    }
}