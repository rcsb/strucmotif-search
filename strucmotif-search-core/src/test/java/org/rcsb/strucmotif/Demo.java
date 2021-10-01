package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class Demo {
    public static void main(String[] args) {
//        MotifSearch.newQuery()
//                // several ways can be used to define the query motif - e.g., specify a PDB entry id
//                .defineByPdbIdAndSelection("4cha",
//                        // and a collection of sequence positions to extract residues to use as motif
//                        List.of(new LabelSelection("B", "1", 42), // HIS
//                                new LabelSelection("B", "1", 87), // ASP
//                                new LabelSelection("C", "1", 47))) // SER
//                // parameters are considered mandatory arguments
//                .rmsdCutoff(1.0)
//                .buildParameters()
//                // retrieve container with complete query
//                .buildQuery()
//                // execute query
//                .run()
//                .getHits()
//                .stream()
//                .map(hit -> hit.getStructureIdentifier() + "_" +
//                        hit.getAssemblyIdentifier() + " @ " +
//                        hit.getLabelSelections() + " - RMSD: " +
//                        hit.getRootMeanSquareDeviation())
//                .forEach(System.out::println);

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("known.list");
        System.out.println(new BufferedReader(new InputStreamReader(inputStream)).lines().map(l -> l.split(",")[0]).collect(Collectors.joining(" ")));
    }
}