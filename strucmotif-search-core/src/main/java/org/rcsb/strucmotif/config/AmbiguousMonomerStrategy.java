package org.rcsb.strucmotif.config;

/**
 * Controls how ambiguous amino acids such as GLX and ASX are handled.
 */
public enum AmbiguousMonomerStrategy {
    /**
     * Ignore and treat as unknown component, effectively disabling search.
     */
    UNKNOWN_COMPONENT,
    /**
     * Treat as unknown type (maps GLX and ASX to the unknown amino acid).
     */
    TYPE,
    /**
     * Treat as their amide equivalent (GLX -> GLN, ASX -> ASN).
     */
    AMIDE,
    /**
     * Treat as their acid equivalent (GLX -> GLU, ASX -> ASP).
     */
    ACID
}
