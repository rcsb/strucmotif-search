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
public class Context {
    final Set<StructureInformation> processed;
    String partitionContext;
    Map<ResiduePairDescriptor, Map<Integer, Collection<ResiduePairIdentifier>>> buffer;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     */
    public Context() {
        this.processed = Collections.synchronizedSet(new HashSet<>());
    }
}
