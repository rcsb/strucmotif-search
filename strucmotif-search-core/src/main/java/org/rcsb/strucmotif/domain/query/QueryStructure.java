package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.Structure;

public interface QueryStructure {
    String getStructureIdentifier();

    Structure getStructure();
}
