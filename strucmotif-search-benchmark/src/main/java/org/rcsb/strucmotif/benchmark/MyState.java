package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.rcsb.strucmotif2.MotifSearch;
import org.rcsb.strucmotif2.Motifs;
import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.query.QueryBuilder;
import org.rcsb.strucmotif2.domain.structure.Structure;
import org.rcsb.strucmotif2.io.read.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
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
    public final Map<Motifs, Structure> structureMap;

    /**
     * Create state.
     */
    public MyState() {
        this.structureMap = Arrays.stream(Motifs.values())
                .collect(Collectors.toMap(Function.identity(), this::createStructure));
    }

    private Structure createStructure(Motifs motif) {
        try {
            URL url = new URL(prepareUri(config.getCifFetchUrl(), motif.getStructureIdentifier()));
            InputStream inputStream = url.openStream();
            return structureReader.readFromInputStream(inputStream, motif.getSelection());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String prepareUri(String raw, StructureIdentifier structureIdentifier) {
        String pdbId = structureIdentifier.getPdbId().toLowerCase();
        String PDBID = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String MIDDLE = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", MIDDLE)
                .replace("{id}", pdbId)
                .replace("{ID}", PDBID);
    }
}