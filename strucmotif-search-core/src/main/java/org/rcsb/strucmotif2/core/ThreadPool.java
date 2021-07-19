package org.rcsb.strucmotif2.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An application-wide shared thread pool with a configurable number of threads. Will be used to operations that perform
 * on parallel streams.
 */
public interface ThreadPool {
    /**
     * Dispatch a Callable.
     * @param task a callable
     * @param <R> type
     * @return a Future aka promise
     */
    <R> Future<R> submit(Callable<R> task);
}
