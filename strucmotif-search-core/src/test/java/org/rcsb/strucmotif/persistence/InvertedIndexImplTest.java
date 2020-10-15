package org.rcsb.strucmotif.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvertedIndexImplTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() {
        invertedIndex = new FileSystemInvertedIndex(new MotifSearchConfig()) {
            @Override
            protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
                // null is okay here
                InputStream inputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("index/" + residuePairDescriptor.toString() + ".msg");
                if (inputStream == null) {
                    throw new IOException();
                }
                return inputStream;
            }
        };
    }

    private static final ResiduePairDescriptor BIN_WITH_ASSEMBLY = new ResiduePairDescriptor(ResidueType.ARGININE,
            ResidueType.ARGININE,
            DistanceType.D15,
            DistanceType.D14,
            AngleType.A80,
            null);

    @Test
    public void whenAccessingSpecificBin_thenObserveAssemblies() {
        assertTrue(invertedIndex.select(BIN_WITH_ASSEMBLY)
                .map(Pair::getSecond)
                .flatMap(Arrays::stream)
                .flatMap(ResiduePairIdentifier::labelSelections)
                .anyMatch(indexSelector -> !indexSelector.getStructOperId().equals("1")));
    }
}