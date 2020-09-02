package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

@Service
public class ThreadPoolImpl implements ThreadPool {
    private final ForkJoinPool pool;

    @Autowired
    public ThreadPoolImpl(MotifSearchConfig motifSearchConfig) {
        this.pool = new ForkJoinPool(motifSearchConfig.getNumberThreads());
    }

    @Override
    public <R> Future<R> submit(Callable<R> task) {
        return pool.submit(task);
    }
}
