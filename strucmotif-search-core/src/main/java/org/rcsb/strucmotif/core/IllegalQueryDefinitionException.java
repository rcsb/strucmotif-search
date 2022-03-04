package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.StrucmotifConfig;

/**
 * Indicates invalid query definitions. May be caused by residue pairs exceeding the distance cutoff
 * ({@link StrucmotifConfig#getDistanceCutoff()}) or erroneous selections (like a mix-up of label_seq_id and
 * auth_seq_id).
 */
public class IllegalQueryDefinitionException extends IllegalArgumentException {
    /**
     * Default constructor.
     */
    public IllegalQueryDefinitionException() {
    }

    /**
     * Construct with message.
     * @param s message
     */
    public IllegalQueryDefinitionException(String s) {
        super(s);
    }

    /**
     * Construct with message and cause.
     * @param message message
     * @param cause the cause
     */
    public IllegalQueryDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct with cause.
     * @param cause the cause
     */
    public IllegalQueryDefinitionException(Throwable cause) {
        super(cause);
    }
}
