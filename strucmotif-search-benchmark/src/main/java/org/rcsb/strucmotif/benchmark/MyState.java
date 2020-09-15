package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.StructureReaderImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
public class MyState {
    private final MotifSearchConfig config = new MotifSearchConfig();
    private final StructureReaderImpl structureReader = new StructureReaderImpl();
    public final QueryBuilder queryBuilder = MotifSearch.newQuery();
    public final Map<Motifs, Structure> structureMap;

    public MyState() {
        this.structureMap = Arrays.stream(Motifs.values())
                .collect(Collectors.toMap(Function.identity(), this::createStructure));
    }

    private Structure createStructure(Motifs motif) {
        try {
            URL url = new URL(String.format(config.getBcifFetchUrl(), motif.getStructureIdentifier().getPdbId()));
            InputStream inputStream = url.openStream();
            return structureReader.readFromInputStream(inputStream, motif.getSelection());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}