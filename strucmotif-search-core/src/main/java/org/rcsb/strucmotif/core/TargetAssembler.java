package org.rcsb.strucmotif.core;

import org.rcsb.strucmotif.domain.StructureSearchContext;

/**
 * Where the magic happens. Lookup all word occurrences which need to be fulfilled and combine them in the most
 * efficient way.
 * <p>Assembles the set of target structures to evaluate by focusing on the paths through targets. Basically, this is
 * subgraph isomorphism: the query is a graph, and we want to find all target structures which contain this query as a
 * subgraph.
 */
public interface TargetAssembler {
    /**
     * Search: i.e. find all paths through all target structures which reasonably resemble the structure of the query
     * motif.
     * @param context the container to work on
     */
    void assemble(StructureSearchContext context);
}
