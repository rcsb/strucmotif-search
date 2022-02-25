package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.MotifDefinition;

import java.io.InputStream;
import java.util.List;

/**
 * Global store of known motifs.
 */
public interface MotifDefinitionRegistry {
    /**
     * Access to all registered motif definitions.
     * @return the collection of registered motifs
     */
    List<MotifDefinition> getMotifDefinitions();

    /**
     * Parse motif definitions from a file/string/URL.
     * @param inputStream stream of a JSON file
     */
    void loadMotifDefinitions(InputStream inputStream);
}
