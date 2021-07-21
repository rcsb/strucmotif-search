package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.List;

public class Demo {
    public static void main(String[] args) {
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify a PDB entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues to use as motif
                        List.of(new LabelSelection("B", "1", 42), // HIS
                                new LabelSelection("B", "1", 87), // ASP
                                new LabelSelection("C", "1", 47))) // SER
                // parameters are considered mandatory arguments
                .buildParameters()
                // retrieve container with complete query
                .buildQuery()
                // execute query
                .run();

//        HDS - grouped BCIF : 12339.8 ms
//        HDS - in-memory index + structures: 12351.5 ms
//        HDS - disk index + in-memory structures: 6831.6 ms up to 12079.0 ms - has some serious GC spikes
//        HDS - disk index + in-memory structures + single thread: 3627.5 ms // TODO investigate thread-count impact on prod system
    }
}
