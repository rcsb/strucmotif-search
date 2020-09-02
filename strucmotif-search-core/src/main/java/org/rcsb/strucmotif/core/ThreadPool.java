package org.rcsb.strucmotif.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface ThreadPool {
    <R> Future<R> submit(Callable<R> task);
}
