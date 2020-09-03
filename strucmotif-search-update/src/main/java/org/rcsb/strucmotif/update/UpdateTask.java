package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

public interface UpdateTask {
    void execute(Collection<StructureIdentifier> delta);
}
