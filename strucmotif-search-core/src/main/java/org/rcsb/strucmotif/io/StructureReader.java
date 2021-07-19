package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.InputStream;

public interface StructureReader {
    Structure readFromInputStream(InputStream inputStream);
}
