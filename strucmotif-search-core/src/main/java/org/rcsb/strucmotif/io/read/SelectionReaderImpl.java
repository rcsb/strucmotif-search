package org.rcsb.strucmotif.io.read;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.persistence.StructureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;

@Service
public class SelectionReaderImpl implements SelectionReader {
    private final StructureRepository structureRepository;

    @Autowired
    public SelectionReaderImpl(StructureRepository structureRepository) {
        this.structureRepository = structureRepository;
    }

    @Override
    public Structure readById(StructureIdentifier structureIdentifier, Collection<IndexSelection> indexSelections) {
        return structureRepository.select(structureIdentifier, indexSelections);
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<IndexSelection> selection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Structure readById(StructureIdentifier structureIdentifier) {
        throw new UnsupportedOperationException();
    }
}
