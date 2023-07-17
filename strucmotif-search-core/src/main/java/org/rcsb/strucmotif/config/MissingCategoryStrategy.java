package org.rcsb.strucmotif.config;

/**
 * How to handle missing categories in the source CIF file.
 */
public enum MissingCategoryStrategy {
    /**
     * Do nothing, use defaults (version will be 1.0, assembly will be "1").
     */
    IGNORE,
    /**
     * Warn with a console statement and use defaults (version will be 1.0, assembly will be "1").
     */
    WARN,
    /**
     * Fail the update if the category is absent.
     */
    FAIL
}
