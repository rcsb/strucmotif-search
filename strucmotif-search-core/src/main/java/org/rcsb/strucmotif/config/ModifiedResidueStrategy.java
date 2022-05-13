package org.rcsb.strucmotif.config;

/**
 * How are modified residues indexed?
 */
public enum ModifiedResidueStrategy {
    /**
     * Ignore all non-standard polymeric components.
     */
    NONE,
    /**
     * Map non-standard polymeric components to their parent component, according to the residue-type-mappings.json
     * file. This is a static, yet overridable variant of CCD_PARENT.
     */
    INTERNAL,
    /**
     * Map non-standard polymeric components to their parent component, according to the chemical component dictionary.
     * This will read the CCD at initialization time.
     */
    CCD_PARENT
}
