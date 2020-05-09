package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The state and results of a motif search run.
 */
public class MotifSearchResult {
    private final MotifSearchQuery query;
    private final Timings timings;

    /**
     * Keep track of the generation we are currently in, i.e. how many words of the query have been consumed.
     */
    private int pathGeneration;
    private Map<StructureIdentifier, TargetStructure> targetStructures;

    private List<Hit> hits;
    private int numberOfPaths;
    private int numberOfTargetStructures;

    public MotifSearchResult(MotifSearchQuery query) {
        this.query = query;
        this.timings = new Timings();
        this.pathGeneration = 0;
        this.hits = new ArrayList<>();
    }

    public MotifSearchQuery getQuery() {
        return query;
    }

    public Timings getTimings() {
        return timings;
    }

    public Map<StructureIdentifier, TargetStructure> getTargetStructures() {
        return targetStructures;
    }

    public void setTargetStructures(Map<StructureIdentifier, TargetStructure> targetStructures) {
        this.targetStructures = targetStructures;
    }

    public void setHits(List<Hit> hits) {
        this.hits = hits;
    }

    public List<Hit> getHits() {
        return hits;
    }

    public void setNumberOfPaths(int numberOfPaths) {
        this.numberOfPaths = numberOfPaths;
    }

    public int getNumberOfPaths() {
        return numberOfPaths;
    }

    public void setNumberOfTargetStructures(int numberOfTargetStructures) {
        this.numberOfTargetStructures = numberOfTargetStructures;
    }

    public int getNumberOfTargetStructures() {
        return numberOfTargetStructures;
    }

    public int incrementAndGetPathGeneration() {
        pathGeneration++;
        return pathGeneration;
    }
}
