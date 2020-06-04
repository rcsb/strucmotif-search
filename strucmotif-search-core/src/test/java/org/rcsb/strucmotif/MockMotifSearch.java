package org.rcsb.strucmotif;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.rcsb.strucmotif.align.Alignment;
import org.rcsb.strucmotif.align.QuaternionAlignment;
import org.rcsb.strucmotif.core.InternalMotifSearch;
import org.rcsb.strucmotif.core.InternalMotifSearchImpl;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifPrunerImpl;
import org.rcsb.strucmotif.core.TargetAssembler;
import org.rcsb.strucmotif.core.TargetAssemblerImpl;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.io.MessagePackCodec;
import org.rcsb.strucmotif.io.MinimizedMessagePackCodec;
import org.rcsb.strucmotif.io.read.AllPurposeReader;
import org.rcsb.strucmotif.io.read.AllPurposeReaderImpl;
import org.rcsb.strucmotif.io.read.MockRenumberedReaderImpl;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.io.read.SelectionReader;
import org.rcsb.strucmotif.io.read.SelectionReaderImpl;
import org.rcsb.strucmotif.io.write.RenumberedWriterImpl;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.MockMongoResidueDBImpl;
import org.rcsb.strucmotif.persistence.MockMotifLookupImpl;
import org.rcsb.strucmotif.persistence.MotifLookup;
import org.rcsb.strucmotif.persistence.MongoResidueDB;

public class MockMotifSearch {
    private static final MockMotifSearch INSTANCE = new MockMotifSearch();
    private final QueryBuilder queryBuilder;

    private MockMotifSearch() {
        Injector injector = Guice.createInjector(new MockModule());
        this.queryBuilder = injector.getInstance(QueryBuilder.class);
    }

    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }

    @SuppressWarnings("Duplicates")
    static class MockModule extends AbstractModule {
        protected void configure() {
            super.configure();
            bind(Alignment.class).to(QuaternionAlignment.class);
            bind(InternalMotifSearch.class).to(InternalMotifSearchImpl.class);
            bind(MotifPruner.class).to(MotifPrunerImpl.class);
            bind(TargetAssembler.class).to(TargetAssemblerImpl.class);
            bind(AllPurposeReader.class).to(AllPurposeReaderImpl.class);
            bind(RenumberedReader.class).to(MockRenumberedReaderImpl.class);
            bind(SelectionReader.class).to(SelectionReaderImpl.class);
            bind(StructureWriter.class).to(RenumberedWriterImpl.class);
            bind(MessagePackCodec.class).to(MinimizedMessagePackCodec.class);
            bind(MongoResidueDB.class).to(MockMongoResidueDBImpl.class);
            bind(MotifLookup.class).to(MockMotifLookupImpl.class);
        }
    }
}
