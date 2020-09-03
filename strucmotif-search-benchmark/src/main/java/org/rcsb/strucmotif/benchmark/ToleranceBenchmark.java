package org.rcsb.strucmotif.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;

@State(Scope.Benchmark)
public class ToleranceBenchmark {
    @Param({"1", "2", "3"})
    public int tolerance;

    @Benchmark
    public void searchForSerineProtease(Blackhole blackhole) {
        blackhole.consume(run(Motifs.HDS, tolerance));
    }

    @Benchmark
    public void searchForAminopeptidase(Blackhole blackhole) {
        blackhole.consume(run(Motifs.KDDDE, tolerance));
    }

    @Benchmark
    public void searchForZincCoordination(Blackhole blackhole) {
        blackhole.consume(run(Motifs.CHH, tolerance));
    }

    @Benchmark
    public void searchForEnolaseSuperfamily(Blackhole blackhole) {
        blackhole.consume(run(Motifs.KDEEH, tolerance));
    }

    @Benchmark
    public void searchForEnolaseSuperfamilyExchanges(Blackhole blackhole) {
        blackhole.consume(run(Motifs.KDEEH_EXCHANGES, tolerance));
    }

    @Benchmark
    public void searchForQuadruplex(Blackhole blackhole) {
        blackhole.consume(run(Motifs.GGGG, tolerance));
    }

    private MotifSearchResult run(Motifs motif, int tolerance) {
        QueryBuilder.OptionalStepBuilder builder = MotifSearch.newQuery()
                .defineByStructure(motif.getStructure())
                .backboneDistanceTolerance(tolerance)
                .sideChainDistanceTolerance(tolerance)
                .angleTolerance(tolerance)
                .buildParameters();

        PositionSpecificExchange[] exchanges = motif.getPositionSpecificExchanges();
        if (exchanges.length > 0) {
            for (PositionSpecificExchange exchange : exchanges) {
                builder.addPositionSpecificExchange(exchange.getLabelSelection(), exchange.getResidueTypes());
            }
        }

        return builder.buildQuery()
            .run();
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ToleranceBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .timeout(TimeValue.days(1))
                .mode(Mode.AverageTime)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
