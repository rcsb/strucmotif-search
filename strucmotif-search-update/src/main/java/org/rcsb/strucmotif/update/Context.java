package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.structure.StructureInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures global state of the update process.
 */
public class Context {
    final List<UpdateItem> updateItems;
    final AtomicInteger batchId;
    final Set<StructureInformation> processed;
    String partitionContext;
    Map<ResiduePairDescriptor, Map<Integer, Collection<ResiduePairIdentifier>>> buffer;
    AtomicInteger structureCounter;

    /**
     * Construct a new update context.
     */
    public Context(List<UpdateItem> updateItems, AtomicInteger batchId) {
        this.updateItems = updateItems;
        this.batchId = batchId;
        this.processed = Collections.synchronizedSet(new HashSet<>());
    }
}
