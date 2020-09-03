package org.rcsb.strucmotif.state;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.Collection;

public interface StateManager {
    Collection<StructureIdentifier> getKnownStructures();

    Collection<StructureIdentifier> getSupportedStructures();
}
