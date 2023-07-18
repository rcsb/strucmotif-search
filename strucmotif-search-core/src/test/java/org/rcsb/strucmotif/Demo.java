package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.List;
import java.util.Set;

public class Demo {
    public static void main(String[] args) {
        // use a single motif definition to search for all structure that contain this motif
        Strucmotif.searchForStructures()
                // several ways can be used to define the query motif - e.g., specify a PDB entry id
                .defineByPdbIdAndSelection("4cha",
                        // and a collection of sequence positions to extract residues to use as motif
                        List.of(new LabelSelection("B", "1", 42), // HIS
                                new LabelSelection("B", "1", 87), // ASP
                                new LabelSelection("C", "1", 47))) // SER
                .rmsdCutoff(1.0)
                .buildParameters()
                .buildContext()
                .run()
                .getHits()
                .stream()
                .map(hit -> hit.structureIdentifier() + "_" +
                        hit.assemblyIdentifier() + " @ " +
                        hit.labelSelections() + " - RMSD: " +
                        hit.rootMeanSquareDeviation())
                .forEach(System.out::println);

        // use a single structure to detect all motifs that occur therein
        Set<EnrichedMotifDefinition> motifs = Strucmotif.getMotifDefinitionRegistry().getEnrichedMotifDefinitions();
        Strucmotif.detectMotifs()
                .defineByPdbIdAndAssemblyId("2mnr", "1")
                .withMotifs(motifs)
                .rmsdCutoff(1.0)
                .buildParameters()
                .buildContext()
                .run()
                .getHits()
                .stream()
                .map(hit -> hit.motifIdentifier() + " @ " +
                        hit.labelSelections() + " - RMSD: " +
                        hit.rootMeanSquareDeviation())
                .forEach(System.out::println);
    }
}