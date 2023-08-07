package org.rcsb.strucmotif.core;

/**
 * Akin to "internal server error".
 */
public class QueryExecutionException extends RuntimeException {
    /**
     * Default constructor.
     */
    public QueryExecutionException() {
    }

    /**
     * Construct with message.
     * @param message msg
     */
    public QueryExecutionException(String message) {
        super(message);
    }

    /**
     * Construct with message and cause.
     * @param message msg
     * @param cause root cause
     */
    public QueryExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct with cause.
     * @param cause root cause
     */
    public QueryExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * Construct with a bunch of info.
     * @param message msg
     * @param cause root cause
     * @param enableSuppression whether suppression is enabled or disabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    public QueryExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
