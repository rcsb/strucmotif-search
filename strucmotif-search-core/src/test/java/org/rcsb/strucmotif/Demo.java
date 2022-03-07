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
                .map(hit -> hit.getStructureIdentifier() + "_" +
                        hit.getAssemblyIdentifier() + " @ " +
                        hit.getLabelSelections() + " - RMSD: " +
                        hit.getRootMeanSquareDeviation())
                .forEach(System.out::println);

        // use a single structure to detect all motifs that occur therein
        Set<EnrichedMotifDefinition> motifs = Strucmotif.getMotifDefinitionRegistry().getEnrichedMotifDefinitions();
        Strucmotif.detectMotifs()
                .defineByPdbId("2mnr", "1")
                .withMotifs(motifs)
                .rmsdCutoff(1.0)
                .buildParameters()
                .buildContext()
                .run()
                .getHits()
                .stream()
                .map(hit -> hit.getMotifIdentifier() + " @ " +
                        hit.getLabelSelections() + " - RMSD: " +
                        hit.getRootMeanSquareDeviation())
                .forEach(System.out::println);
    }
}