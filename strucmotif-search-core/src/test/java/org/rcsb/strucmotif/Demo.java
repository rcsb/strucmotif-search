package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.Set;

public class Demo {
    public static void main(String[] args) {
        // the entry point for all things motif search - #newQuery() starts building a new query
        MotifSearch.newQuery()
                // several ways can be used to define the query motif - e.g., specify a PDB entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues to use as motif
                        Set.of(new LabelSelection("B", "1", 42), // HIS
                               new LabelSelection("B", "1", 87), // ASP
                               new LabelSelection("C", "1", 47))) // SER
                // parameters are considered mandatory arguments
                .buildParameters()
                // retrieve container with complete query
                .buildQuery()
                // execute query
                .run();
    }
}
