package org.rcsb.strucmotif2;

import org.rcsb.strucmotif2.domain.query.ScoringStrategy;
import org.rcsb.strucmotif2.domain.result.TransformedHit;
import org.rcsb.strucmotif2.domain.selection.LabelSelection;

import java.util.Set;

public class Demo {
    public static void main(String[] args) {
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify a PDB entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues to use as motif
                        Set.of(new LabelSelection("B", "1", 42), // HIS
                                new LabelSelection("B", "1", 87), // ASP
                                new LabelSelection("C", "1", 47))) // SER
                .scoringStrategy(ScoringStrategy.ALIGNMENT)
                // parameters are considered mandatory arguments
                .buildParameters()
                // retrieve container with complete query
                .buildQuery()
                // execute query
                .run()
                .getHits()
                .stream()
                .limit(1)
                .map(TransformedHit.class::cast)
                .forEach(h -> {
                    System.out.println(h.getSelection());
                    System.out.println(h.getResidueTypes());
                });

//        List<Long> times = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            // the entry point for all things motif search - #newQuery() starts building a new query
//            MotifSearchResult result = MotifSearch.newQuery()
//                    // several ways can be used to define the query motif - e.g., specify a PDB entry id
//                    .defineByPdbIdAndSelection("4cha",
//                            // and a collection of sequence positions to extract residues to use as motif
//                            Set.of(new LabelSelection("B", "1", 42), // HIS
//                                    new LabelSelection("B", "1", 87), // ASP
//                                    new LabelSelection("C", "1", 47))) // SER
//                    .scoringStrategy(ScoringStrategy.ALIGNMENT)
//                    // parameters are considered mandatory arguments
//                    .buildParameters()
//                    // retrieve container with complete query
//                    .buildQuery()
//                    // execute query
//                    .run();
//            long time = result.getTimings().getQueryTime();
//            times.add(time);
//        }
//        System.out.println(times.stream().mapToLong(l -> l).average().getAsDouble() + " ms");

//        HDS - grouped BCIF : 12339.8 ms
//        HDS - in-memory index + structures: 12351.5 ms
//        HDS - disk index + in-memory structures: 6831.6 ms up to 12079.0 ms - has some serious GC spikes
//        HDS - disk index + in-memory structures + single thread: 3627.5 ms // TODO investigate thread-count impact on prod system
    }
}
