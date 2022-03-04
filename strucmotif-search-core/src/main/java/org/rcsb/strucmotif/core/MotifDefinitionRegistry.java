package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

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

    /**
     * Return an 'enriched' representation (i.e. with all necessary structure data) of all known motifs.
     * @return the function to obtain structure data
     */
    List<EnrichedMotifDefinition> getEnrichedMotifDefinitions(Function<MotifDefinition, EnrichedMotifDefinition> mapper);
}
