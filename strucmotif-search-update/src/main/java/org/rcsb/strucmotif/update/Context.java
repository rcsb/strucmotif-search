package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.StructureInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures global state of the update process.
 */
class Context {
    final Set<String> known;
    final Set<StructureInformation> processed;
    String partitionContext;
    Map<ResiduePairDescriptor, Map<String, Collection<ResiduePairIdentifier>>> buffer;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     * @param known set of registered structures
     */
    public Context(Set<String> known) {
        this.known = known;
        this.processed = Collections.synchronizedSet(new HashSet<>());
    }
}
