package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.Structure;

public class MotifSearchBenchmark {
    @Benchmark
    public void searchForCatalyticTriad(Blackhole blackhole, MyState state) {
        // catalytic activity - the trivial case
        blackhole.consume(run(Motifs.HDS, state));
    }

    @Benchmark
    public void searchForAminoPeptidase(Blackhole blackhole, MyState state) {
        blackhole.consume(run(Motifs.KDDDE, state));
    }

    @Benchmark
    public void searchForIonCoordination(Blackhole blackhole, MyState state) {
        // ion fixation - 3 residues with ambiguity
        blackhole.consume(run(Motifs.CHH, state));
    }

    @Benchmark
    public void searchForSuperfamilyTemplate(Blackhole blackhole, MyState state) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(Motifs.KDEEH, state));
    }

    @Benchmark
    public void searchForSuperfamilyTemplateExchanges(Blackhole blackhole, MyState state) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(Motifs.KDEEH_EXCHANGES, state));
    }

    @Benchmark
    public void searchForRNAComplex(Blackhole blackhole, MyState state) {
        // RNA interaction motif - 4 residues with total ambiguity
        blackhole.consume(run(Motifs.GGGG, state));
    }

    private MotifSearchResult run(Motifs motif, MyState state) {
        Structure structure = state.structureMap.get(motif);
        return state.queryBuilder.defineByStructure(structure)
                .buildParameters()
                .buildQuery()
                .run();
    }

    /*
    Benchmark                                                          Mode  Cnt  Score   Error  Units
    MotifSearchBenchmark.searchForAminoPeptidase                avgt   10  0.494 ± 0.128   s/op
    MotifSearchBenchmark.searchForCatalyticTriad                avgt   10  0.869 ± 0.087   s/op
    MotifSearchBenchmark.searchForIonCoordination               avgt   10  0.126 ± 0.005   s/op
    MotifSearchBenchmark.searchForRNAComplex                    avgt   10  1.093 ± 0.041   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplate           avgt   10  0.336 ± 0.007   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplateExchanges  avgt   10  1.236 ± 0.194   s/op
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MotifSearchBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .timeout(TimeValue.days(1))
                .mode(Mode.AverageTime)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
