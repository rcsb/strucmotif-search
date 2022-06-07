package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Global store of known motifs. This loads a hard-coded collection of motifs as well as will screen for a file called
 * 'motifs.json' located on the classpath. Additionally, you can manually register, or deregister, motifs. The service
 * keeps track of the global state of registered motifs. It also has the ability to enrich motifs with structure data.
 */
public interface MotifDefinitionRegistry {
    /**
     * Parse motif definitions from a file/string/URL.
     * @param inputStream stream of a JSON file
     * @return  if content changed
     */
    boolean parseAndAddMotifDefinitions(InputStream inputStream);

    /**
     * Register a motif.
     * @param motifDefinition what to add
     * @return true if content changed
     */
    boolean addMotifDefinition(MotifDefinition motifDefinition);

    /**
     * Register multiple motifs.
     * @param motifDefinitions what to add
     * @return true if content changed
     */
    boolean addMotifDefinitions(Collection<MotifDefinition> motifDefinitions);

    /**
     * Remove a motif.
     * @param motifDefinition what to remove
     * @return true if content changed
     */
    boolean removeMotifDefinition(MotifDefinition motifDefinition);

    /**
     * Remove multiple motifs.
     * @param motifDefinitions what to remove
     * @return true if content changed
     */
    boolean removeMotifDefinitions(Collection<MotifDefinition> motifDefinitions);

    /**
     * Conditionally remove.
     * @param predicate condition for removal
     * @return true if content changed
     */
    boolean removeMotifDefinitions(Predicate<? super MotifDefinition> predicate);

    /**
     * All registered motif definitions.
     * @return a collection of motif definitions
     */
    Set<MotifDefinition> getMotifDefinitions();

    /**
     * Number of registered motifs.
     * @return an int
     */
    int size();

    /**
     * Access to all 'enriched' representation (i.e. with all necessary structure data) of all known motifs. Defaults to
     * loading data from local or public resources. Use {@link #enrichMotifDefinitions(Function mapper)} for motifs
     * from non-archived structures.
     * @return collection of motifs with resolved structure data
     */
    Set<EnrichedMotifDefinition> getEnrichedMotifDefinitions();

    /**
     * Specify how 'enriched' representation (i.e. with all necessary structure data) are created.
     * @param mapper the function to obtain structure data
     * @return collection motif motifs with resolved structure data
     */
    Set<EnrichedMotifDefinition> enrichMotifDefinitions(Function<MotifDefinition, EnrichedMotifDefinition> mapper);

    /**
     * The default enricher that attaches structure data to motif definitions. This is done by loading local or remote
     * structure data. For structure without structure data in the usual places use
     * {@link #enrichMotifDefinitions(Function) enrichMotifDefinitions} and provide an appropriate implementation.
     * @return the default enricher
     */
    Function<MotifDefinition, EnrichedMotifDefinition> getDefaultEnricher();
}
