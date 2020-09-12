package org.rcsb.strucmotif.config;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties
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
    private int maxMotifSize = 10;
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

    /**
     * Acquire input stream of 'some' structure file. Priority are: 1. 'data-source' 2. 'root-path', and lastly fetched
     * from 'bcif-fetch-url'.
     * @param structureIdentifier the structure to load
     * @return structure data
     */
    public InputStream getInputStream(StructureIdentifier structureIdentifier) {
        try {
            Path dataSourcePath = Paths.get(dataSource).resolve(structureIdentifier.getPdbId().toLowerCase() + ".bcif.gz");
            return Files.newInputStream(dataSourcePath);
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath().resolve(structureIdentifier.getPdbId() + ".bcif.gz");
                return Files.newInputStream(renumberedPath);
            } catch (IOException e2) {
                try {
                    return new URL(String.format(bcifFetchUrl, structureIdentifier.getPdbId())).openStream();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    public Path getRenumberedStructurePath() {
        return Paths.get(rootPath).resolve("renumbered");
    }

    public InputStream getRenumberedInputStream(StructureIdentifier structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath().resolve(structureIdentifier.getPdbId() + ".bcif.gz"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public double getSquaredDistanceCutoff() {
        return distanceCutoff * distanceCutoff;
    }

}
