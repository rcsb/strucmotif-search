package org.rcsb.strucmotif.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "strucmotif")
public class MotifSearchConfig {
    private double distanceCutoff = 20;
    private String rootPath = "/opt/data/";
    private String dataSource = "/opt/pdb/";
    private int numberThreads = Runtime.getRuntime().availableProcessors();
    private String dbConnectionUri = "mongodb://localhost:27017/?";
    private int maxResults = 10000;
    private int decimalPlacesRmsd = 2;
    private int decimalPlacesMatrix = 3;
    private int chunkSize = 400;

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

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public int getNumberThreads() {
        return numberThreads;
    }

    public void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public String getDbConnectionUri() {
        return dbConnectionUri;
    }

    public void setDbConnectionUri(String dbConnectionUri) {
        this.dbConnectionUri = dbConnectionUri;
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

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Path getArchivePath() {
        return Paths.get(rootPath).resolve("archive");
    }

    public double getSquaredDistanceCutoff() {
        return distanceCutoff * distanceCutoff;
    }
}
