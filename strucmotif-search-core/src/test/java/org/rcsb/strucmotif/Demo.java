package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.List;

public class Demo {
    public static void main(String[] args) {
        MotifSearchResult run = MotifSearch.newQuery()
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

        // TODO 32 KB residueMapping
        System.out.println(run); // 61.61 KB
    }
}
