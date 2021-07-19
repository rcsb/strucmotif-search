package org.rcsb.strucmotif2.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.rcsb.strucmotif2.domain.Pair;
import org.rcsb.strucmotif2.domain.motif.AngleType;
import org.rcsb.strucmotif2.domain.motif.DistanceType;
import org.rcsb.strucmotif2.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif2.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif2.domain.structure.ResidueType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvertedIndexImplTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() {
        invertedIndex = new InvertedIndexImpl(new MotifSearchConfig()) {
            @Override
            protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor
//                    , boolean b
            ) throws IOException {
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

    private static final ResiduePairDescriptor BIN_WITH_ASSEMBLY = new ResiduePairDescriptor(ResidueType.ASPARTIC_ACID,
            ResidueType.LYSINE,
            DistanceType.D6,
            DistanceType.D7,
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