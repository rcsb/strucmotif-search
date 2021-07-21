package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Setup needed to benchmark.
 */
@State(Scope.Benchmark)
public class MyState {
    private final MotifSearchConfig config = new MotifSearchConfig();
    private final StructureReaderImpl structureReader = new StructureReaderImpl();
    public final QueryBuilder queryBuilder = MotifSearch.newQuery();
    public final Map<Motifs, Pair<Structure, List<LabelSelection>>> structureMap;

    /**
     * Create state.
     */
    public MyState() {
        this.structureMap = Arrays.stream(Motifs.values())
                .collect(Collectors.toMap(Function.identity(), this::createStructure));
    }

    private Pair<Structure, List<LabelSelection>> createStructure(Motifs motif) {
        try {
            URL url = new URL(prepareUri(config.getCifFetchUrl(), motif.getStructureIdentifier()));
            InputStream inputStream = url.openStream();
            return new Pair<>(structureReader.readFromInputStream(inputStream), motif.getLabelSelections());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String prepareUri(String raw, String structureIdentifier) {
        String pdbId = structureIdentifier.toLowerCase();
        String PDBID = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String MIDDLE = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", MIDDLE)
                .replace("{id}", pdbId)
                .replace("{ID}", PDBID);
    }
}