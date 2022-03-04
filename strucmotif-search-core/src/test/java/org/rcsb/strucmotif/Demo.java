package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

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
        List<EnrichedMotifDefinition> motifs = loadMotifs();
        Strucmotif.detectMotifs()
                .defineByPdbId("2mnr", "1")
                .andMotifs(motifs)
                .rmsdCutoff(1.0)
                .buildParameters()
                .buildContext()
                .run()
                .getHits()
                .stream()
                .map(hit -> hit.getMotifIdentifier() + "_" +
                        hit.getLabelSelections() + " - RMSD: " +
                        hit.getRootMeanSquareDeviation())
                .forEach(System.out::println);
    }

    private static List<EnrichedMotifDefinition> loadMotifs() {
        return Strucmotif.getMotifDefinitionRegistry()
                .getEnrichedMotifDefinitions(Demo::enricher);
    }

    private static final StructureReader STRUCTURE_READER = new StructureReaderImpl();
    private static EnrichedMotifDefinition enricher(MotifDefinition motifDefinition) {
        try {
            // this assumes that the motif is part of the PDB, otherwise point to your data source
            InputStream inputStream = new URL("https://models.rcsb.org/" + motifDefinition.getStructureIdentifier() + ".bcif").openStream();
            // load into a Structure object
            Structure structure = STRUCTURE_READER.readFromInputStream(inputStream);
            // create/obtain coordinates for all relevant residues
            List<Map<LabelAtomId, float[]>> residues = structure.manifestResidues(motifDefinition.getLabelSelections());
            return new EnrichedMotifDefinition(motifDefinition, structure, residues);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}