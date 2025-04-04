package org.rcsb.strucmotif.config;

/**
 * Configures how read errors to the file bundles are handled. These seem to be caused by heavy load on the service.
 * Though the lack of exceptions leading up to the error might indicate that the root cause is bad concurrency.
 */
public enum ReadErrorStrategy {
    THROW, // throw exception at query level, ignore at service
    EXIT, // throw exception at query level, terminate compromised service
    REINITIALIZE // reinitialize compromised file bundle as needed
}
