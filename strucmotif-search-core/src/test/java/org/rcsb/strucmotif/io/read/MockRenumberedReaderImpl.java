package org.rcsb.strucmotif.io.read;

import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

public class MockRenumberedReaderImpl extends RenumberedReaderImpl {
    private static final Logger logger = LoggerFactory.getLogger(MockRenumberedReaderImpl.class);

    public MockRenumberedReaderImpl() {
        logger.info("mocking renumbered structure reading");
    }

    @Override
    public Structure readById(String pdbId, Collection<IndexSelection> selection) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("bcif/" + pdbId + ".bcif");
        Objects.requireNonNull(inputStream, "unable to acquire InputStream of renumbered bcif for " + pdbId);
        return readFromInputStream(inputStream, selection);
    }
}
