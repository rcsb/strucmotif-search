package org.rcsb.strucmotif.io.read;

import com.google.inject.Singleton;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

@Singleton
public class MockAllPurposeReaderImpl extends AllPurposeReaderImpl {
    private static final Logger logger = LoggerFactory.getLogger(MockAllPurposeReaderImpl.class);

    public MockAllPurposeReaderImpl() {
        logger.debug("Mocking original structure reading");
    }

    @Override
    public Structure readById(String pdbId, Collection<LabelSelection> selection) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("orig/" + pdbId + ".bcif");
        Objects.requireNonNull(inputStream, "unable to acquire InputStream of original bcif for " + pdbId);
        return readFromInputStream(inputStream, selection);
    }
}
