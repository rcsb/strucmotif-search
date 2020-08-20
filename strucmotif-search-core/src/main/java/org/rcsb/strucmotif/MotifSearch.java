package org.rcsb.strucmotif;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.rcsb.strucmotif.align.Alignment;
import org.rcsb.strucmotif.align.QuaternionAlignment;
import org.rcsb.strucmotif.core.*;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.MessagePackCodec;
import org.rcsb.strucmotif.io.MinimizedMessagePackCodec;
import org.rcsb.strucmotif.io.read.*;
import org.rcsb.strucmotif.io.write.RenumberedWriterImpl;
import org.rcsb.strucmotif.io.write.StructureWriter;
import org.rcsb.strucmotif.persistence.MongoClientHolder;
import org.rcsb.strucmotif.persistence.MongoClientHolderImpl;
import org.rcsb.strucmotif.persistence.MongoInvertedIndexImpl;
import org.rcsb.strucmotif.persistence.MongoResidueDBImpl;
import org.rcsb.strucmotif.persistence.MongoTitleDB;
import org.rcsb.strucmotif.persistence.MongoTitleDBImpl;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.FileSystemInvertedIndexImpl;
import org.rcsb.strucmotif.persistence.MongoResidueDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ForkJoinPool;

/**
 * The entry point to perform motif searches.
 */
public class MotifSearch {
    private static final Logger logger = LoggerFactory.getLogger(MotifSearch.class);
    public static final AbstractModule MOTIF_SEARCH_MODULE = new AbstractModule() {
        @Override
        protected void configure() {
            super.configure();
            bind(Alignment.class).to(QuaternionAlignment.class);
            bind(InternalMotifSearch.class).to(InternalMotifSearchImpl.class);
            bind(MotifPruner.class).to(MotifPrunerImpl.class);
            bind(TargetAssembler.class).to(TargetAssemblerImpl.class);
            bind(AllPurposeReader.class).to(AllPurposeReaderImpl.class);
            bind(RenumberedReader.class).to(RenumberedReaderImpl.class);
            bind(SelectionReader.class).to(NO_MONGO_DB ? FileSystemSelectionReaderImpl.class : MongoSelectionReaderImpl.class);
            bind(StructureWriter.class).to(RenumberedWriterImpl.class);
            bind(MessagePackCodec.class).to(MinimizedMessagePackCodec.class);
            bind(MongoResidueDB.class).to(NO_MONGO_DB ? null : MongoResidueDBImpl.class);
            bind(MongoTitleDB.class).to(NO_MONGO_DB ? null : MongoTitleDBImpl.class);
            bind(InvertedIndex.class).to(NO_MONGO_DB ? FileSystemInvertedIndexImpl.class : MongoInvertedIndexImpl.class);
            bind(MongoClientHolder.class).to(MongoClientHolderImpl.class);
        }
    };

    public static final double DISTANCE_CUTOFF;
    public static final double DISTANCE_CUTOFF_SQUARED;

    private static final Path DATA_ROOT;
    public static final Path ARCHIVE_PATH;
    public static final Path LOOKUP_PATH;
    public static final Path ARCHIVE_LIST;
    public static final Path LOOKUP_LIST;
    public static final Path RESIDUE_LIST;

    public static final boolean NO_MONGO_DB;

    public static final ForkJoinPool FORK_JOIN_POOL;
    private static final DecimalFormat DECIMAL_FORMAT;
    private static final MotifSearch INSTANCE = new MotifSearch();

    private final QueryBuilder queryBuilder;

    private MotifSearch() {
        Injector injector = Guice.createInjector(MOTIF_SEARCH_MODULE);
        this.queryBuilder = injector.getInstance(QueryBuilder.class);
    }

    public static QueryBuilder newQuery() {
        return INSTANCE.queryBuilder;
    }

    public static String format(Object object) {
        return DECIMAL_FORMAT.format(object);
    }

    static {
        logger.info("Setting motif search constants");
        try (InputStream input = MotifSearch.class.getClassLoader().getResourceAsStream("config.properties")) {
            Objects.requireNonNull(input, "Did not find property file: 'config.properties' on classpath");
            Properties prop = new Properties();
            prop.load(input);

            int threads = Runtime.getRuntime().availableProcessors();
            logger.debug("Will use {} threads", threads);
            FORK_JOIN_POOL = new ForkJoinPool(threads);

            // the cutoff up to which words are detected
            DISTANCE_CUTOFF = Double.parseDouble(prop.getProperty("distance.cutoff"));
            DISTANCE_CUTOFF_SQUARED = DISTANCE_CUTOFF * DISTANCE_CUTOFF;

            // the root path of all service data
            DATA_ROOT = Paths.get(prop.getProperty("path.root"));
            // an optimized archive - minimal information, maximum IO performance for whole files
            ARCHIVE_PATH = DATA_ROOT.resolve("archive").resolve("bcif-renum");
            // the location of the lookup
            LOOKUP_PATH = DATA_ROOT.resolve("lookup");
            // keeps track of all files for which a reduced/optimized coordinate file exists
            ARCHIVE_LIST = DATA_ROOT.resolve("archive.list");
            // all structures currently present in the index (may be empty structures - processed but not containing any valid words)
            LOOKUP_LIST = DATA_ROOT.resolve("lookup.list");
            // all structures currently indexed in the component-DB
            RESIDUE_LIST = DATA_ROOT.resolve("component.list");

            NO_MONGO_DB = Boolean.parseBoolean(prop.getProperty("no.mongodb"));
            if (NO_MONGO_DB) {
                logger.info("Coordinates will be read from bcif - enable MongoDB for improved performance");
            }

            DECIMAL_FORMAT = new DecimalFormat(prop.getProperty("decimal.format"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        logger.debug("Data root path is {}", DATA_ROOT);
    }
}
