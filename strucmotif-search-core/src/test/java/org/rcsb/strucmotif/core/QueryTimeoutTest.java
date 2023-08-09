package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.AbstractSearchContext;
import org.rcsb.strucmotif.domain.SearchContext;
import org.rcsb.strucmotif.domain.query.Parameters;
import org.rcsb.strucmotif.domain.query.QueryStructure;
import org.rcsb.strucmotif.domain.query.SearchQuery;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.SearchResult;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryTimeoutTest {
    private static final Logger logger = LoggerFactory.getLogger(QueryTimeoutTest.class);
    private static final int EXECUTOR_TIMEOUT = 10;
    private StrucmotifConfig strucmotifConfig;
    private DefaultStrucmotifRuntime runtime;
    private SearchContext<?, ?, ?> context;
    private boolean threadsKeptRunning;

    @BeforeEach
    void init() {
        strucmotifConfig = new StrucmotifConfig();
        strucmotifConfig.setQueryTimeout(10);
        runtime = new DefaultStrucmotifRuntime(null, strucmotifConfig, null);
        threadsKeptRunning = false;

        context = new AbstractSearchContext<>(new ForkJoinPool(strucmotifConfig.getPerQueryThreads())) {
            @Override
            public StrucmotifRuntime getRuntime() {
                return null;
            }

            @Override
            public StrucmotifConfig getConfig() {
                return strucmotifConfig;
            }

            @Override
            public SearchQuery<Parameters, QueryStructure> getQuery() {
                return new SearchQuery<>() {
                    @Override
                    public Parameters getParameters() {
                        return new Parameters() {
                            @Override
                            public int getTimeout() {
                                return EXECUTOR_TIMEOUT;
                            }

                            @Override
                            public boolean hasTimeout() {
                                return true;
                            }
                        };
                    }

                    @Override
                    public QueryStructure getQueryStructure() {
                        return null;
                    }
                };
            }

            @Override
            public SearchResult<Hit> run() {
                return null;
            }

            @Override
            public void runAndConsume(Consumer<Hit> hitConsumer) {

            }

            @Override
            public SearchResult<Hit> getResult() {
                return null;
            }

            @Override
            public StructureIndexProvider getStructureIndexProvider() {
                return null;
            }

            @Override
            public StructureDataProvider getStructureDataProvider() {
                return null;
            }

            @Override
            public InvertedIndex getInvertedIndex() {
                return null;
            }

            @Override
            protected String composeHeader() {
                return null;
            }

            @Override
            protected String composeOutput(Hit hit) {
                return null;
            }
        };
    }

    @Test
    void whenLongRunningTask_thenTimeoutExceptionThrown() throws InterruptedException {
        int delay = 100;
        Runnable task = () -> sleep(delay);
        assertThrows(QueryTimeoutException.class, () -> runtime.runTimeoutAware(context, task));
        assertRunnableDidntLinger(delay);
    }

    @Test
    void whenParallelStream_thenTimeoutExceptionThrown() throws InterruptedException {
        int delay = 100;
        assertThrows(QueryTimeoutException.class, () -> runtime.runTimeoutAware(context, () -> {
            int sum = IntStream.range(0, 10 * Math.min(2, Runtime.getRuntime().availableProcessors()))
                    .parallel()
                    .map(i -> sleep(delay))
                    .sum();
            logger.warn("Allowed to process parallel tasks up to {}", sum);
        }));
        assertRunnableDidntLinger(delay);
    }

    private int sleep(int millis) {
        try {
            Thread.sleep(millis);
            logger.warn("Thread was not terminated properly, resources are leaking");
            threadsKeptRunning = true;
            return millis;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return millis;
        }
    }

    private void assertRunnableDidntLinger(int millis) throws InterruptedException {
        Thread.sleep(millis);
        Thread.sleep(millis);
        assertFalse(threadsKeptRunning);
    }
}