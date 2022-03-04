package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.StructureSearchContext;
import org.rcsb.strucmotif.domain.MotifSearchContext;
import org.rcsb.strucmotif.domain.result.StructureHit;
import org.rcsb.strucmotif.domain.result.MotifHit;

import java.util.function.Consumer;

/**
 * Performs motif search queries and returns the corresponding result object.
 */
public interface MotifSearchRuntime {
    /**
     * Perform a structure search.
     * @param context a structure context
     */
    void performSearch(StructureSearchContext context);

    /**
     * Perform a structure search and consume results 'inline'.
     * @param context a structure context
     * @param consumer consumer of structure hits
     */
    void performSearch(StructureSearchContext context, Consumer<StructureHit> consumer);

    /**
     * Perform a structure search.
     * @param context a motif context
     */
    void performSearch(MotifSearchContext context);

    /**
     * Perform a motif search and consume results 'inline'.
     * @param context a motif context
     * @param consumer consumer of motif hits
     */
    void performSearch(MotifSearchContext context, Consumer<MotifHit> consumer);
}
