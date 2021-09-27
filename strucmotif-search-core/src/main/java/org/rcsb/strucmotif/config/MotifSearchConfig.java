package org.rcsb.strucmotif.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * All properties used throughout the strucmotif-search application.
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "strucmotif")
public class MotifSearchConfig {
    /**
     * The maximum distance in Å between alpha carbon atoms of residue pairs. All pairs below will be added the inverted
     * index and can appear as search results. Lower values ease storage requirements and improve speed of update
     * operations.
     */
    private float distanceCutoff = 15;
    /**
     * The root directory where optimized BinaryCIF data will be written.
     */
    private String rootPath = "/opt/data-af/";
    /**
     * Optional path to a local collection of structure data. This will be used during update operations. If not set or
     * not valid, corresponding data will be fetched from <code>bcif-fetch-url</code>. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     */
    private String dataSource = "/opt/pdb/{middle}/{id}.bcif.gz";
    /**
     * How many threads should be used during multi-threaded operations (update, path assembly, structure reading).
     */
    private int numberThreads = Runtime.getRuntime().availableProcessors();
    /**
     * Hard limit on the number of results returned. Will stop jobs when this number of hits has been accepted. Acts as
     * a safeguard against too simple queries that will return an overwhelming number of results.
     */
    private int maxResults = 10000;
    /**
     * How many decimal places to report for scores.
     */
    private int decimalPlacesScore = 2;
    /**
     * How many decimal places to report for entries of transformation matrices.
     */
    private int decimalPlacesMatrix = 3;
    /**
     * The batch size during update. Writing to the inverted index is expensive, therefore doing so in batches increases
     * speed substantially. A value of 400 works good with 12GB of heap, the higher the faster.
     */
    private int updateChunkSize = 1600;
    /**
     * The maximum motif size, any larger user input will be rejected.
     */
    private int maxMotifSize = 10;
    /**
     * The URL where (Binary)CIF data will be fetched from if not present locally. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     */
    private String cifFetchUrl = "https://models.rcsb.org/{id}.bcif";
    /**
     * What precision to use to write renumbered structure data.
     */
    private int renumberedCoordinatePrecision = 1;
    /**
     * Gzip renumbered files? Yields smaller files that are slower to read.
     */
    private boolean renumberedGzip = true;
    /**
     * Number of allowed tries during file download before an {@link java.io.IOException} is thrown.
     */
    private int downloadTries = 1;
    /**
     * Allocate a lot of memory to keep index and structure data in memory?
     */
    private InMemoryStrategy inMemoryStrategy = InMemoryStrategy.OFF;
    /**
     * Allow hits that are not part of an assembly (e.g. relevant for NMR or computed structure models).
     * Hits without assembly are report as '0'.
     */
    private boolean undefinedAssemblies = true;
    /**
     * Filter for residues with a certain quality/confidence. Only relevant when combined with
     * {@link ResidueQualityStrategy}.
     */
    private float residueQualityCutoff = 70.0f;
    /**
     * Filter for residues with a certain quality/confidence. Update 'qualityThreshold' accordingly to use this.
     */
    private ResidueQualityStrategy residueQualityStrategy = ResidueQualityStrategy.NONE;
    /**
     * List of all identifiers ever registered.
     */
    public static final String STATE_KNOWN_LIST = "known.list";
    /**
     * List of all identifiers needed to recover if update operation fails.
     */
    public static final String STATE_DIRTY_LIST = "dirty.list";
    /**
     * Name of the renumbered directory.
     */
    public static final String RENUMBERED_DIRECTORY = "renumbered";
    /**
     * Name of the inverted index directory.
     */
    public static final String INDEX_DIRECTORY = "index";
    /**
     * RCSB URL that reports currently present identifiers in archive.
     */
    public static final String RCSB_ENTRY_LIST = "https://data.rcsb.org/rest/v1/holdings/current/entry_ids";

    /**
     * Maximum distance of residue pairs.
     * @return threshold in Angstrom
     */
    public float getDistanceCutoff() {
        return distanceCutoff;
    }

    /**
     * Set maximum distance of residue pairs.
     * @param distanceCutoff threshold in Angstrom
     */
    public void setDistanceCutoff(float distanceCutoff) {
        this.distanceCutoff = distanceCutoff;
    }

    /**
     * The root path where data is read/written.
     * @return a path
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Set the root path where data is read/written.
     * @param rootPath a path
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Optional path to a local collection of structure data. This will be used during update operations. If not set or
     * not valid, corresponding data will be fetched from <code>bcif-fetch-url</code>. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     * @return a path
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * Optional path to a local collection of structure data. This will be used during update operations. If not set or
     * not valid, corresponding data will be fetched from <code>bcif-fetch-url</code>. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     * @param dataSource a path
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * How many threads to use?
     * @return an int
     */
    public int getNumberThreads() {
        return numberThreads;
    }

    /**
     * Set how many threads to use?
     * @param numberThreads an int >0
     */
    public void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    /**
     * Maximum number of results returned.
     * @return an int
     */
    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Set maximum number of results returned.
     * @param maxResults an int
     */
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    /**
     * How many digits to write (e.g. for RMSD).
     * @return an int
     */
    public int getDecimalPlacesScore() {
        return decimalPlacesScore;
    }

    /**
     * Set how many digits to write (e.g. for RMSD).
     * @param decimalPlacesScore an int
     */
    public void setDecimalPlacesScore(int decimalPlacesScore) {
        this.decimalPlacesScore = decimalPlacesScore;
    }

    /**
     * How many digits to write for matrices.
     * @return an int
     */
    public int getDecimalPlacesMatrix() {
        return decimalPlacesMatrix;
    }

    /**
     * Set how many digits to write for matrices.
     * @param decimalPlacesMatrix an int
     */
    public void setDecimalPlacesMatrix(int decimalPlacesMatrix) {
        this.decimalPlacesMatrix = decimalPlacesMatrix;
    }

    /**
     * How many structures to process before flushing to inverted index.
     * @return an int
     */
    public int getUpdateChunkSize() {
        return updateChunkSize;
    }

    /**
     * Set how many structures to process before flushing to inverted index.
     * @param updateChunkSize an int
     */
    public void setUpdateChunkSize(int updateChunkSize) {
        this.updateChunkSize = updateChunkSize;
    }

    /**
     * Maximum number of residues in a motif.
     * @return an int
     */
    public int getMaxMotifSize() {
        return maxMotifSize;
    }

    /**
     * Set maximum number of residues in a motif.
     * @param maxMotifSize an int
     */
    public void setMaxMotifSize(int maxMotifSize) {
        this.maxMotifSize = maxMotifSize;
    }

    /**
     * The URL where (Binary)CIF data will be fetched from if not present locally. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     * @return a URL
     */
    public String getCifFetchUrl() {
        return cifFetchUrl;
    }

    /**
     * The URL where (Binary)CIF data will be fetched from if not present locally. <code>{id}</code> refers to the
     * PDB-ID, <code>{middle}</code> can be used to represent the middle hash of an ID (e.g. 'ab' for '1abc').
     * @param cifFetchUrl a URL
     */
    public void setCifFetchUrl(String cifFetchUrl) {
        this.cifFetchUrl = cifFetchUrl;
    }

    /**
     * Convenience method to get squared distance cutoff.
     * @return a float
     */
    public float getSquaredDistanceCutoff() {
        return distanceCutoff * distanceCutoff;
    }

    /**
     * How many digits to write for renumbered files.
     * @return an int
     */
    public int getRenumberedCoordinatePrecision() {
        return renumberedCoordinatePrecision;
    }

    /**
     * Set how many digits to write for renumbered files.
     * @param renumberedCoordinatePrecision an int
     */
    public void setRenumberedCoordinatePrecision(int renumberedCoordinatePrecision) {
        this.renumberedCoordinatePrecision = renumberedCoordinatePrecision;
    }

    /**
     * Gzip renumbered files?
     * @return a Boolean
     */
    public boolean isRenumberedGzip() {
        return renumberedGzip;
    }

    /**
     * Set gzip renumbered files?
     * @param renumberedGzip a Boolean
     */
    public void setRenumberedGzip(boolean renumberedGzip) {
        this.renumberedGzip = renumberedGzip;
    }

    /**
     * How many tries are allowed for connection failures/timeouts during update.
     * @return an int
     */
    public int getDownloadTries() {
        return downloadTries;
    }

    /**
     * Set how many tries should be allowed for connection failures/timeouts during update.
     * @param downloadTries an int
     */
    public void setDownloadTries(int downloadTries) {
        this.downloadTries = downloadTries;
    }

    /**
     * How is index and structure data acquired at runtime?
     * @return the InMemoryStrategy
     */
    public InMemoryStrategy getInMemoryStrategy() {
        return inMemoryStrategy;
    }

    /**
     * Set how index and structure data is acquired at runtime.
     * @param inMemoryStrategy the new value
     */
    public void setInMemoryStrategy(InMemoryStrategy inMemoryStrategy) {
        this.inMemoryStrategy = inMemoryStrategy;
    }

    /**
     * Are hits without assembly allowed?
     * @return a Boolean
     */
    public boolean isUndefinedAssemblies() {
        return undefinedAssemblies;
    }

    /**
     * Set if hits without assembly are allowed.
     * @param undefinedAssemblies true to allow
     */
    public void setUndefinedAssemblies(boolean undefinedAssemblies) {
        this.undefinedAssemblies = undefinedAssemblies;
    }

    /**
     * Filter residues with 'quality' at this threshold. Will evaluate values written to the B-Factor column in the
     * source CIF.
     * @return a float value
     */
    public float getResidueQualityCutoff() {
        return residueQualityCutoff;
    }

    /**
     * Set the 'quality' filter threshold.
     * @param residueQualityCutoff the value that separates meaningful and to-be-ignored residues
     */
    public void setResidueQualityCutoff(float residueQualityCutoff) {
        this.residueQualityCutoff = residueQualityCutoff;
    }

    /**
     * The used quality strategy.
     * @return ResidueQualityStrategy
     */
    public ResidueQualityStrategy getResidueQualityStrategy() {
        return residueQualityStrategy;
    }

    /**
     * Update the quality strategy.
     * @param residueQualityStrategy the new value
     */
    public void setResidueQualityStrategy(ResidueQualityStrategy residueQualityStrategy) {
        this.residueQualityStrategy = residueQualityStrategy;
    }
}
