package org.rcsb.strucmotif.benchmark.io;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.InvertedIndexImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InvertedIndexRunner {
    private static final int BINS_READ = 100;

    public static void main(String[] args) throws IOException {
        MotifSearchConfig motifSearchConfig = new MotifSearchConfig();
        Path path = Paths.get(motifSearchConfig.getRootPath()).resolve(MotifSearchConfig.INDEX_DIRECTORY);
        List<ResiduePairDescriptor> descriptors = Files.walk(path)
                .parallel()
                // ignore directories
                .filter(p -> !Files.isDirectory(p))
                .map(InvertedIndexRunner::createResiduePairDescriptor)
                .collect(Collectors.toList());

        InvertedIndexImpl invertedIndex = new InvertedIndexImpl(motifSearchConfig);
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            Collections.shuffle(descriptors);
            List<ResiduePairDescriptor> selected = descriptors.subList(0, BINS_READ);

            long c = 0;
            for (ResiduePairDescriptor descriptor : selected) {
                c += invertedIndex.select(descriptor).count();

            }
            long time = (System.nanoTime() - start) / 1000 / 1000;
            times.add(time);
            System.out.println("Read " + c + " elements in " + time + " ms");
        }

        System.out.println();
        System.out.println("Average throughput:");
        System.out.println(times.stream().mapToLong(l -> l).average().orElseThrow());

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
        //Average throughput:
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
        //Average throughput:
        //227.7
    }

    private static final Map<String, ResidueType> OLC_LOOKUP = Stream.of(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getOneLetterCode, Function.identity()));
    private static ResiduePairDescriptor createResiduePairDescriptor(Path path) {
        String name = path.toFile().getName();
        String[] split = name.split("\\.")[0].split("-");
        ResidueType residueType1 = OLC_LOOKUP.getOrDefault(split[0].substring(0, 1), null);
        ResidueType residueType2 = OLC_LOOKUP.getOrDefault(split[0].substring(1, 2), null);
        DistanceType d1 = DistanceType.ofIntRepresentation(Integer.parseInt(split[1]));
        DistanceType d2 = DistanceType.ofIntRepresentation(Integer.parseInt(split[2]));
        AngleType a = AngleType.ofIntRepresentation(Integer.parseInt(split[3]));
        return new ResiduePairDescriptor(residueType1, residueType2, d1, d2, a);
    }
}
