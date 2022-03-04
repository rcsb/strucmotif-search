package org.rcsb.strucmotif.benchmark.integration;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
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
import org.rcsb.strucmotif.domain.query.StructureContextBuilder;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.util.List;
import java.util.Set;

/**
 * Generic benchmark via JMH.
 */
public class StructureSearchBenchmark {
    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForCatalyticTriad(Blackhole blackhole, MyState state) {
        // catalytic activity - the trivial case
        blackhole.consume(run(MotifDefinition.HDS, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForAminoPeptidase(Blackhole blackhole, MyState state) {
        blackhole.consume(run(MotifDefinition.KDDDE, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForIonCoordination(Blackhole blackhole, MyState state) {
        // ion fixation - 3 residues with ambiguity
        blackhole.consume(run(MotifDefinition.CHH, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForSuperfamilyTemplate(Blackhole blackhole, MyState state) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(MotifDefinition.KDEEH, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForSuperfamilyTemplateExchanges(Blackhole blackhole, MyState state) {
        // superfamily template - 5 residues with ambiguity
        blackhole.consume(run(MotifDefinition.KDEEH_EXCHANGES, state));
    }

    /**
     * A benchmark.
     * @param blackhole consume results (avoid dead-code elimination)
     * @param state state
     */
    @Benchmark
    public void searchForRNAComplex(Blackhole blackhole, MyState state) {
        // RNA interaction motif - 4 residues with total ambiguity
        blackhole.consume(run(MotifDefinition.GGGG, state));
    }

    private StructureSearchResult run(MotifDefinition motif, MyState state) {
        Pair<Structure, List<LabelSelection>> structure = state.structureMap.get(motif);
        StructureContextBuilder.OptionalBuilderStep builder = state.queryBuilder.defineByStructureAndSelection(structure.getFirst(), structure.getSecond())
                .atomPairingScheme(AtomPairingScheme.ALL)
                .rmsdCutoff(2.0f)
                .buildParameters();

        Set<PositionSpecificExchange> exchanges = motif.getPositionSpecificExchanges();
        if (exchanges.size() > 0) {
            for (PositionSpecificExchange exchange : exchanges) {
                builder.addPositionSpecificExchange(exchange.getLabelSelection(), exchange.getResidueTypes());
            }
        }

        return builder.buildContext().run();
    }

    /*
    Jul 2021 - 0.11.2 - score by RMSD, coordinates read on-the-fly from local BinaryCIF
    MotifSearchBenchmark.searchForAminoPeptidase                avgt   10    1.193 ±  0.138   s/op
    MotifSearchBenchmark.searchForCatalyticTriad                avgt   10   12.482 ±  3.426   s/op
    MotifSearchBenchmark.searchForIonCoordination               avgt   10    0.491 ±  0.037   s/op
    MotifSearchBenchmark.searchForRNAComplex                    avgt   10  135.017 ± 19.194   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplate           avgt   10    0.682 ±  0.034   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplateExchanges  avgt   10    0.689 ±  0.037   s/op

    Jul 2021 - 0.11.2 - geometric scores
    MotifSearchBenchmark.searchForAminoPeptidase                avgt   10  0.888 ± 0.049   s/op
    MotifSearchBenchmark.searchForCatalyticTriad                avgt   10  1.020 ± 0.076   s/op
    MotifSearchBenchmark.searchForIonCoordination               avgt   10  0.113 ± 0.009   s/op
    MotifSearchBenchmark.searchForRNAComplex                    avgt   10  0.520 ± 0.032   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplate           avgt   10  0.618 ± 0.054   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplateExchanges  avgt   10  0.761 ± 0.349   s/op

    Feb 2020 - 0.0.1 - coordinates from MongoDB
    Benchmark                                                          Mode  Cnt  Score   Error  Units
    MotifSearchBenchmark.searchForAminoPeptidase                avgt   10  0.494 ± 0.128   s/op
    MotifSearchBenchmark.searchForCatalyticTriad                avgt   10  0.869 ± 0.087   s/op
    MotifSearchBenchmark.searchForIonCoordination               avgt   10  0.126 ± 0.005   s/op
    MotifSearchBenchmark.searchForRNAComplex                    avgt   10  1.093 ± 0.041   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplate           avgt   10  0.336 ± 0.007   s/op
    MotifSearchBenchmark.searchForSuperfamilyTemplateExchanges  avgt   10  1.236 ± 0.194   s/op
     */

    /**
     * Entry point.
     * @param args nothing
     * @throws RunnerException benchmark failed
     */
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(StructureSearchBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(10)
                .timeout(TimeValue.days(1))
                .mode(Mode.AverageTime)
                .forks(1)
                .build();
        new Runner(options).run();
    }
}
