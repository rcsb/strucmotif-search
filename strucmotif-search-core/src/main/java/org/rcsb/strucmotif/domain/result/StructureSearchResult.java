package org.rcsb.strucmotif.domain.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The results of a structure search run.
 */
public class StructureSearchResult implements SearchResult<StructureHit> {
    private final Timings timings;

    /**
     * Keep track of the generation we are currently in, i.e. how many words of the query have been consumed.
     */
    private int pathGeneration;
    private Map<Integer, TargetStructure> targetStructures;

    private List<StructureHit> hits;
    private int numberOfPaths;
    private int numberOfTargetStructures;

    /**
     * Construct a result container.
     */
    public StructureSearchResult() {
        this.timings = new Timings();
        this.pathGeneration = 0;
        this.hits = new ArrayList<>();
    }

    /**
     * Associated timings.
     * @return timings object
     */
    @Override
    public Timings getTimings() {
        return timings;
    }

    /**
     * Access to all currently referenced target structures.
     * @return a map with structure identifiers as keys and target structure instances as values
     */
    public Map<Integer, TargetStructure> getTargetStructures() {
        return targetStructures;
    }

    /**
     * Update the currently referenced target structures.
     * @param targetStructures a map with structure indices as keys and target structure instances as values
     */
    public void setTargetStructures(Map<Integer, TargetStructure> targetStructures) {
        this.targetStructures = targetStructures;
    }

    /**
     * Update the collection of currently valid hits.
     * @param hits a collection of hits
     */
    public void setHits(List<StructureHit> hits) {
        this.hits = hits;
    }

    /**
     * Access all (currently) valid hits.
     * @return a collection of hits
     */
    @Override
    public List<StructureHit> getHits() {
        return hits;
    }

    /**
     * Update the number of valid paths.
     * @param numberOfPaths an int
     */
    public void setNumberOfPaths(int numberOfPaths) {
        this.numberOfPaths = numberOfPaths;
    }

    /**
     * The number of valid paths in all target structures.
     * @return an int
     */
    public int getNumberOfPaths() {
        return numberOfPaths;
    }

    /**
     * Update the number of referenced target structures.
     * @param numberOfTargetStructures an int
     */
    public void setNumberOfTargetStructures(int numberOfTargetStructures) {
        this.numberOfTargetStructures = numberOfTargetStructures;
    }

    /**
     * The number of referenced target structures.
     * @return an int
     */
    public int getNumberOfTargetStructures() {
        return numberOfTargetStructures;
    }

    /**
     * Called internally after a descriptor has been processed.
     * @return the new path generation
     */
    public int incrementAndGetPathGeneration() {
        pathGeneration++;
        return pathGeneration;
    }
}
