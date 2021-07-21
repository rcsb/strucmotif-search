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

        //initial timings with label_asym_id in data
        //Read 557295 elements in 454 ms
        //Read 247221 elements in 237 ms
        //Read 587850 elements in 350 ms
        //Read 412914 elements in 263 ms
        //Read 380211 elements in 294 ms
        //Read 642704 elements in 448 ms
        //Read 507038 elements in 323 ms
        //Read 391000 elements in 341 ms
        //Read 630326 elements in 511 ms
        //Read 479498 elements in 290 ms
        //
        //Average throughput:
        //351.1
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
