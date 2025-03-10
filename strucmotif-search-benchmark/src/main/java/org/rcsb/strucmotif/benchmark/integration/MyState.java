package org.rcsb.strucmotif.benchmark.integration;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.rcsb.strucmotif.Strucmotif;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.query.StructureContextBuilder;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.DefaultStructureReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Setup needed to benchmark.
 */
@State(Scope.Benchmark)
public class MyState {
    private final StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
    private final StructureReader structureReader = new DefaultStructureReader(new DefaultResidueTypeResolver(strucmotifConfig));
    final StructureContextBuilder queryBuilder = Strucmotif.searchForStructures();
    final Map<MotifDefinition, Pair<Structure, List<LabelSelection>>> structureMap;

    /**
     * Create state.
     */
    public MyState() {
        List<MotifDefinition> motifs = List.of(MotifDefinition.CHH,
                MotifDefinition.CHCH,
                MotifDefinition.GGGG,
                MotifDefinition.HDS,
                MotifDefinition.KDDDE,
                MotifDefinition.KDEEH,
                MotifDefinition.KDEEH_EXCHANGES);
        this.structureMap = motifs.stream()
                .collect(Collectors.toMap(Function.identity(), this::createStructure));
    }

    private Pair<Structure, List<LabelSelection>> createStructure(MotifDefinition motif) {
        try {
            URL url = new URL(prepareUri(strucmotifConfig.getCifFetchUrl(), motif.getStructureIdentifier()));
            InputStream inputStream = url.openStream();
            return new Pair<>(structureReader.readFromInputStream(inputStream), motif.getLabelSelections());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("Duplicates")
    private String prepareUri(String raw, String structureIdentifier) {
        String pdbId = structureIdentifier.toLowerCase();
        String pdbIdUc = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String middleUc = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", middleUc)
                .replace("{id}", pdbId)
                .replace("{ID}", pdbIdUc);
    }
}