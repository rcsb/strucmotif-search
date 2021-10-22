package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InvertedIndexImplTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() {
        invertedIndex = new InvertedIndexImpl(new MotifSearchConfig()) {
            @Override
            protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
                // null is okay here
                InputStream inputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("index/" + residuePairDescriptor + ".msg.gz");
                if (inputStream == null) {
                    throw new IOException();
                }
                return new GZIPInputStream(inputStream);
            }
        };
    }

    private static final ResiduePairDescriptor BIN_WITH_ASSEMBLY = new ResiduePairDescriptor(ResidueType.ASPARTIC_ACID,
            ResidueType.LYSINE,
            DistanceType.D6,
            DistanceType.D7,
            AngleType.A80);

    @Test
    public void whenAccessingSpecificBin_thenObserveAssemblies() {
        InvertedIndexBucket bucket = invertedIndex.select(BIN_WITH_ASSEMBLY);
        int nonIdentity = 0;
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();
                if (!bucket.getStructOperId1().equals("1")) {
                    nonIdentity++;
                }
                if (!bucket.getStructOperId2().equals("1")) {
                    nonIdentity++;
                }
            }
        }
        assertTrue(nonIdentity > 0);
    }
}