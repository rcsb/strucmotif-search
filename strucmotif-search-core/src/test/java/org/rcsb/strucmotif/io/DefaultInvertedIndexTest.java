package org.rcsb.strucmotif.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.ThreadPool;
import org.rcsb.strucmotif.core.DefaultThreadPool;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.codec.ColferCodec;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultInvertedIndexTest {
    private InvertedIndex invertedIndex;

    @BeforeEach
    public void init() throws IOException {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        ThreadPool threadPool = new DefaultThreadPool(strucmotifConfig);
        ReadableFileBundle fileBundle = FileBundleIO.openBundle(Helpers.getResourceAsPath("index.data"), Helpers.getResourceAsPath("index.ffindex")).inReadOnlyMode();
        ColferCodec bucketCodec = new ColferCodec();
        invertedIndex = new DefaultInvertedIndex(threadPool, strucmotifConfig) {
            @Override
            public InvertedIndexBucket select(int residuePairDescriptor) {
                String filename = residuePairDescriptor + ".colf";
                if (!fileBundle.containsFile(filename)) {
                    return InvertedIndexBucket.EMPTY_BUCKET;
                }

                try {
                    return bucketCodec.decode(fileBundle.readFile(filename));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    private static final int BIN_WITH_ASSEMBLY = ResiduePairDescriptor.encodeDescriptor(ResidueType.ASPARTIC_ACID,
            ResidueType.LYSINE,
            DistanceType.D6,
            DistanceType.D7,
            AngleType.A80);

    @Test
    void whenAccessingSpecificBin_thenObserveAssemblies() {
        InvertedIndexBucket bucket = invertedIndex.select(BIN_WITH_ASSEMBLY);
        int structures = 0;
        int occurrences = 0;
        while (bucket.hasNextStructure()) {
            bucket.moveStructure();
            structures++;
            while (bucket.hasNextOccurrence()) {
                bucket.moveOccurrence();
                occurrences++;
            }
        }
        assertEquals(13, structures);
        assertEquals(241, occurrences);
    }

    @Test
    void whenSelectingByFlippedDescriptor_thenContentMatchesOriginal() {
        InvertedIndexBucket original = invertedIndex.select(BIN_WITH_ASSEMBLY);
        InvertedIndexBucket flipped = invertedIndex.select(BIN_WITH_ASSEMBLY & ~ResiduePairDescriptor.FLIPPED_MASK);
        assertArrayEquals(original.getStructureIndexArray(), flipped.getStructureIndexArray());
        assertArrayEquals(original.getPositionOffsetArray(), flipped.getPositionOffsetArray());
        assertArrayEquals(original.getIdentifierDataArray(), flipped.getIdentifierDataArray());
    }
}