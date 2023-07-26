package org.rcsb.strucmotif.benchmark.io;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.DefaultInvertedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads bins of the inverted index to gauge performance.
 */
public class InvertedIndexRunner {
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndexRunner.class);
    private static final int BINS_READ = 100;

    /**
     * Entry point.
     * @param args nothing
     */
    public static void main(String[] args) {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();

        InvertedIndex invertedIndex = new DefaultInvertedIndex(strucmotifConfig);
        List<Integer> descriptors = new ArrayList<>(invertedIndex.reportKnownDescriptors());
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            Collections.shuffle(descriptors);
            List<Integer> selected = descriptors.subList(0, BINS_READ);

            long c = 0;
            for (int descriptor : selected) {
                c += invertedIndex.select(descriptor).getStructureCount();

            }
            long time = (System.nanoTime() - start) / 1000 / 1000;
            times.add(time);
            logger.info("Read {} elements in {}} ms", c, time);
        }

        logger.info("");
        double avg = times.stream().mapToLong(l -> l).average().orElseThrow();
        logger.info("Average time: {}", avg);

        //2021-10-27 14:39:15.286 [main] INFO  org.rcsb.strucmotif.io.InvertedIndexImpl - Index files will not be gzipped - extension: .msg
        //Read 616421 elements in 216 ms
        //Read 497808 elements in 118 ms
        //Read 481498 elements in 110 ms
        //Read 534879 elements in 114 ms
        //Read 640341 elements in 126 ms
        //Read 733743 elements in 139 ms
        //Read 876002 elements in 168 ms
        //Read 707873 elements in 143 ms
        //Read 765972 elements in 158 ms
        //Read 645154 elements in 121 ms
        //
        //Average time:
        //141.3

        //2021-10-04 11:11:01.270 [main] INFO  org.rcsb.strucmotif.io.InvertedIndexImpl - Index files will be gzipped - extension: .msg.gz
        //Read 552360 elements in 3534 ms
        //Read 513735 elements in 3199 ms
        //Read 539428 elements in 3738 ms
        //Read 721375 elements in 5275 ms
        //Read 558558 elements in 3794 ms
        //Read 761731 elements in 5991 ms
        //Read 632861 elements in 4733 ms
        //Read 698068 elements in 5189 ms
        //Read 753067 elements in 5831 ms
        //Read 516964 elements in 3981 ms
        //
        //Average time:
        //4526.5

        //2021-10-04 11:18:54.911 [main] INFO  org.rcsb.strucmotif.io.InvertedIndexImpl - Index files will not be gzipped - extension: .msg
        //Read 587244 elements in 270 ms
        //Read 587985 elements in 261 ms
        //Read 808860 elements in 269 ms
        //Read 560564 elements in 223 ms
        //Read 792576 elements in 282 ms
        //Read 532212 elements in 179 ms
        //Read 538596 elements in 201 ms
        //Read 544274 elements in 190 ms
        //Read 572425 elements in 243 ms
        //Read 520086 elements in 159 ms
        //
        //Average time:
        //227.7
    }
}
