package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Wraps a ForkJoinPool.
 */
@Service
public class DefaultThreadPool implements ThreadPool {
    private final ForkJoinPool pool;

    /**
     * Constructor.
     * @param strucmotifConfig injectable configuration
     */
    @Autowired
    public DefaultThreadPool(StrucmotifConfig strucmotifConfig) {
        this.pool = new ForkJoinPool(strucmotifConfig.getNumberThreads());
    }

    @Override
    public <R> Future<R> submit(Callable<R> task) {
        return pool.submit(task);
    }
}
