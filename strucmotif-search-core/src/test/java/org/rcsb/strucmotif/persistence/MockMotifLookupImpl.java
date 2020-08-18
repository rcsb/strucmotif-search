package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.io.MessagePackCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@Singleton
public class MockMotifLookupImpl extends MotifLookupImpl {
    private static final Logger logger = LoggerFactory.getLogger(MockMotifLookupImpl.class);

    @Inject
    public MockMotifLookupImpl(MessagePackCodec messagePackCodec) {
        super(messagePackCodec);
        logger.debug("Mocking motif lookup");
    }

    @Override
    protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("lookup/" + residuePairDescriptor + ".lu");
        if (inputStream == null) {
            // this is not bad in expected cases (i.e. RR-16-13-3.lu)
            throw new IOException("missing/empty file");
        }
        return inputStream;
    }
}
