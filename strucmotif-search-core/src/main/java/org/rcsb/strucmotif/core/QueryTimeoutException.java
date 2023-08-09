package org.rcsb.strucmotif.core;

/**
 * Reports that a query was terminated because it timed out.
 */
public class QueryTimeoutException extends RuntimeException {
    /**
     * Default constructor.
     */
    public QueryTimeoutException() {
    }

    /**
     * Construct with a detailed message.
     * @param message msg
     */
    public QueryTimeoutException(String message) {
        super(message);
    }
}
