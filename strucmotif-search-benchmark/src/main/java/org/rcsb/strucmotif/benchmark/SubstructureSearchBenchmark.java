package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;

public class SubstructureSearchBenchmark {
    @Benchmark
    public void searchForCatalyticTriad(Blackhole blackhole) {
        // catalytic activity - the trivial case
        blackhole.consume(run(Motifs.HDS));
    }

    @Benchmark
    public void searchForAminoPeptidase(Blackhole blackhole) {
        blackhole.consume(run(Motifs.KDDDE));
    }

    @Benchmark
    public void searchForIonCoordination(Blackhole blackhole) {
        // ion fixation - 3 residues with ambiguity
        blackhole.consume(run(Motifs.CHH));
    }

    @Benchmark
    public void searchForSuperfamilyTemplate(Blackhole blackhole) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(Motifs.KDEEH));
    }

    @Benchmark
    public void searchForSuperfamilyTemplateExchanges(Blackhole blackhole) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(Motifs.KDEEH_EXCHANGES));
    }

    @Benchmark
    public void searchForRNAComplex(Blackhole blackhole) {
        // RNA interaction motif - 4 residues with total ambiguity
        blackhole.consume(run(Motifs.GGGG));
    }

    private MotifSearchResult run(Motifs motif) {
        return MotifSearch.newQuery()
                .defineByStructure(motif.getStructure())
                .buildParameters()
                .buildQuery()
                .run();
    }

    /*
    Benchmark                                                          Mode  Cnt  Score   Error  Units
    SubstructureSearchBenchmark.searchForAminoPeptidase                avgt   10  0.494 ± 0.128   s/op
    SubstructureSearchBenchmark.searchForCatalyticTriad                avgt   10  0.869 ± 0.087   s/op
    SubstructureSearchBenchmark.searchForIonCoordination               avgt   10  0.126 ± 0.005   s/op
    SubstructureSearchBenchmark.searchForRNAComplex                    avgt   10  1.093 ± 0.041   s/op
    SubstructureSearchBenchmark.searchForSuperfamilyTemplate           avgt   10  0.336 ± 0.007   s/op
    SubstructureSearchBenchmark.searchForSuperfamilyTemplateExchanges  avgt   10  1.236 ± 0.194   s/op
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SubstructureSearchBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .timeout(TimeValue.days(1))
                .mode(Mode.AverageTime)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
