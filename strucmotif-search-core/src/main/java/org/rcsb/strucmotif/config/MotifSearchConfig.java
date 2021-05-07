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
     * index and can appear as search results. 18 is really generous, lower values ease storage requirements and improve
     * speed of update operations.
     */
    private double distanceCutoff = 15;
    /**
     * The root directory where optimized BinaryCIF data will be written.
     */
    private String rootPath = "/opt/data/";
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
    private int updateChunkSize = 400;
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
     * Number of allowed retries during file download before an {@link java.io.IOException} is thrown.
     */
    private int downloadRetries = 1;
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
     * RCSB URL that reports currently identifiers in archive.
     */
    public static final String RCSB_ENTRY_LIST = "https://data.rcsb.org/rest/v1/holdings/current/entry_ids";

    /**
     * Maximum distance of residue pairs.
     * @return threshold in Angstrom
     */
    public double getDistanceCutoff() {
        return distanceCutoff;
    }

    /**
     * Set maximum distance of residue pairs.
     * @param distanceCutoff threshold in Angstrom
     */
    public void setDistanceCutoff(double distanceCutoff) {
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
     * @return a double
     */
    public double getSquaredDistanceCutoff() {
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
     * How many retries are allowed for connection failures/timeouts during update.
     * @return an int
     */
    public int getDownloadRetries() {
        return downloadRetries;
    }

    /**
     * Set how many retries should be allowed for connection failures/timeouts during update.
     * @param downloadRetries an int
     */
    public void setDownloadRetries(int downloadRetries) {
        this.downloadRetries = downloadRetries;
    }
}
