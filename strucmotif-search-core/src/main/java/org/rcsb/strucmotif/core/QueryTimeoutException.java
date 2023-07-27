package org.rcsb.strucmotif.core;

public class QueryTimeoutException extends RuntimeException {
    public QueryTimeoutException() {
    }

    public QueryTimeoutException(String message) {
        super(message);
    }
}
