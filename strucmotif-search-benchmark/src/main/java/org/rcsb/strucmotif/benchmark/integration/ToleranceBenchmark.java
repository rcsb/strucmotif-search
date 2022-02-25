package org.rcsb.strucmotif.benchmark.integration;

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
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;
import java.util.Set;

/**
 * Tolerance-specific benchmark via JMH.
 */
@State(Scope.Benchmark)
public class ToleranceBenchmark {
    @Param({"1", "2", "3"})
    public int tolerance;

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForSerineProtease(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.HDS, tolerance, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForAminopeptidase(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.KDDDE, tolerance, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForZincCoordination(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.CHH, tolerance, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForEnolaseSuperfamily(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.KDEEH, tolerance, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForEnolaseSuperfamilyExchanges(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.KDEEH_EXCHANGES, tolerance, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForQuadruplex(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.GGGG, tolerance, state));
    }

    private MotifSearchResult run(MotifDefinition motif, int tolerance, MyState state) {
        Pair<Structure, List<LabelSelection>> structure = state.structureMap.get(motif);
        QueryBuilder.OptionalStepBuilder builder = state.queryBuilder.defineByStructureAndSelection(structure.getFirst(), structure.getSecond())
                .atomPairingScheme(AtomPairingScheme.ALL)
                .rmsdCutoff(2.0f)
                .backboneDistanceTolerance(tolerance)
                .sideChainDistanceTolerance(tolerance)
                .angleTolerance(tolerance)
                .buildParameters();

        Set<PositionSpecificExchange> exchanges = motif.getPositionSpecificExchanges();
        if (exchanges.size() > 0) {
            for (PositionSpecificExchange exchange : exchanges) {
                builder.addPositionSpecificExchange(exchange.getLabelSelection(), exchange.getResidueTypes());
            }
        }

        return builder.buildQuery()
            .run();
    }

    /**
     * Entry point for benchmarks.
     * @param args arguments
     * @throws RunnerException benchmark failure
     */
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
