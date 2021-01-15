package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.config.MotifSearchConfig;

/**
 * Indicates invalid query definitions. May be caused by residue pairs exceeding the distance cutoff
 * ({@link MotifSearchConfig#getDistanceCutoff()}) or erroneous selections (like a mix-up of label_seq_id and
 * auth_seq_id).
 */
public class IllegalQueryDefinitionException extends IllegalArgumentException {
    public IllegalQueryDefinitionException() {
    }

    public IllegalQueryDefinitionException(String s) {
        super(s);
    }

    public IllegalQueryDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalQueryDefinitionException(Throwable cause) {
        super(cause);
    }
}
