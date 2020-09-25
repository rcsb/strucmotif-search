package org.rcsb.strucmotif.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "strucmotif")
public class MotifSearchConfig {
    /**
     * The maximum distance in Å between alpha carbon atoms of residue pairs. All pairs below will be added the inverted
     * index and can appear as search results. 20 is really generous, lower values ease storage requirements and improve
     * speed of update operations.
     */
    private double distanceCutoff = 20;
    /**
     * The root directory where optimized BinaryCIF data will be written.
     */
    private String rootPath = "/opt/data/";
    /**
     * Optional path to a local collection of structure data. This will be used during update operations. If not set or
     * not valid, corresponding data will be fetched from <code>bcif-fetch-url</code>.
     */
    private String dataSource = "/opt/pdb/";
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
     * How many decimal places to report for RMSD values.
     */
    private int decimalPlacesRmsd = 2;
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
     * The number of residues that will be written into 1 structure file. Smaller values result in better performance
     * but also will create more files.
     */
    private int structureChunkSize = 250;
    /**
     * The maximum motif size, any larger user input will be rejected.
     */
    private int maxMotifSize = 10;
    /**
     * The URL where BinaryCIF data will be fetched from if not present locally.
     */
    private String bcifFetchUrl = "https://models.rcsb.org/%s.bcif";

    public double getDistanceCutoff() {
        return distanceCutoff;
    }

    public void setDistanceCutoff(double distanceCutoff) {
        this.distanceCutoff = distanceCutoff;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getDataSource() {
        return dataSource;
    }

    public Path getDataSourcePath() {
        return Paths.get(dataSource);
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public int getNumberThreads() {
        return numberThreads;
    }

    public void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getDecimalPlacesRmsd() {
        return decimalPlacesRmsd;
    }

    public void setDecimalPlacesRmsd(int decimalPlacesRmsd) {
        this.decimalPlacesRmsd = decimalPlacesRmsd;
    }

    public int getDecimalPlacesMatrix() {
        return decimalPlacesMatrix;
    }

    public void setDecimalPlacesMatrix(int decimalPlacesMatrix) {
        this.decimalPlacesMatrix = decimalPlacesMatrix;
    }

    public int getUpdateChunkSize() {
        return updateChunkSize;
    }

    public void setUpdateChunkSize(int updateChunkSize) {
        this.updateChunkSize = updateChunkSize;
    }

    public int getStructureChunkSize() {
        return structureChunkSize;
    }

    public void setStructureChunkSize(int structureChunkSize) {
        this.structureChunkSize = structureChunkSize;
    }

    public int getMaxMotifSize() {
        return maxMotifSize;
    }

    public void setMaxMotifSize(int maxMotifSize) {
        this.maxMotifSize = maxMotifSize;
    }

    public String getBcifFetchUrl() {
        return bcifFetchUrl;
    }

    public void setBcifFetchUrl(String bcifFetchUrl) {
        this.bcifFetchUrl = bcifFetchUrl;
    }

    public double getSquaredDistanceCutoff() {
        return distanceCutoff * distanceCutoff;
    }
}
