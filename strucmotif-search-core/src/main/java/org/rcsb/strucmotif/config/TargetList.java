package org.rcsb.strucmotif.config;

/**
 * Controls the set of allowed targets, effectively providing high-level control to find exclusively PDB structures, or
 * exclusively computed structure models.
 */
public enum TargetList {
    /**
     * Return only PDB-entries.
     */
    PDB,
    /**
     * Return only computed structure models.
     */
    MODELS,
    /**
     * Return all hits, regardless of provenance.
     */
    ALL
}
