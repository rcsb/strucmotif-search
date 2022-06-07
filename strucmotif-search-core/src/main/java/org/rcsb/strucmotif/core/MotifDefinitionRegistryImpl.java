package org.rcsb.strucmotif.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An in-memory store of all known/registered motifs.
 */
@Service
public class MotifDefinitionRegistryImpl implements MotifDefinitionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MotifDefinitionRegistryImpl.class);
    private final StructureDataProvider structureDataProvider;
    private final Set<MotifDefinition> motifDefinitions;
    private final AtomicBoolean dirty; // true if motifs & enriched motifs are out-of-sync
    private Set<EnrichedMotifDefinition> enrichedMotifDefinitions;

    // Lazy initialization if no JSON is desired
    static class GsonHolder {
        static final Gson instance = new Gson();
    }

    static class ListTypeHolder {
        static final Type instance = new TypeToken<ArrayList<MotifDefinition>>(){}.getType();
    }

    /**
     * Constructs a registry. Will auto-register all hard-coded motifs from {@link MotifDefinition} and also read
     * `motifs.json` on the classpath and parse all motifs from it.
     * @param structureDataProvider source of structure data
     */
    @Autowired
    public MotifDefinitionRegistryImpl(StructureDataProvider structureDataProvider) {
        this.structureDataProvider = structureDataProvider;
        this.motifDefinitions = new HashSet<>();
        this.dirty = new AtomicBoolean(false);

        init();
    }

    private void init() {
        logger.info("Initializing motif definition registry");

        logger.debug("Loading hard-coded definitions");
        try {
            for (Field field : MotifDefinition.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !field.getType().equals(MotifDefinition.class)) continue;
                motifDefinitions.add((MotifDefinition) field.get(MotifDefinition.class));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        logger.debug("Parsing definitions from motifs.json");
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("motifs.json");
        parseAndAddMotifDefinitions(inputStream);

        logger.info("{} motifs registered", motifDefinitions.size());
        dirty.set(true);
    }

    @Override
    public boolean parseAndAddMotifDefinitions(InputStream inputStream) {
        List<MotifDefinition> motifs = GsonHolder.instance.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), ListTypeHolder.instance);
        return addMotifDefinitions(motifs);
    }

    @Override
    public boolean addMotifDefinition(MotifDefinition motifDefinition) {
        boolean changed = motifDefinitions.add(motifDefinition);
        if (changed) sync(this::defaultMapping);
        return changed;
    }

    @Override
    public boolean addMotifDefinitions(Collection<MotifDefinition> motifDefinitions) {
        boolean changed = this.motifDefinitions.addAll(motifDefinitions);
        if (changed) sync(this::defaultMapping);
        return changed;
    }

    @Override
    public boolean removeMotifDefinition(MotifDefinition motifDefinition) {
        boolean changed = motifDefinitions.remove(motifDefinition);
        if (changed) sync(this::defaultMapping);
        return changed;
    }

    @Override
    public boolean removeMotifDefinitions(Collection<MotifDefinition> motifDefinitions) {
        boolean changed = this.motifDefinitions.removeAll(motifDefinitions);
        if (changed) sync(this::defaultMapping);
        return changed;
    }

    @Override
    public boolean removeMotifDefinitions(Predicate<? super MotifDefinition> predicate) {
        boolean changed = motifDefinitions.removeIf(predicate);
        if (changed) sync(this::defaultMapping);
        return changed;
    }

    @Override
    public Set<MotifDefinition> getMotifDefinitions() {
        return motifDefinitions;
    }

    @Override
    public int size() {
        return motifDefinitions.size();
    }

    /**
     * Acquire needed structure data for a motif.
     * @param motifDefinition original motif definition
     * @return motif definition with structure data
     * @throws UncheckedIOException if no matching local or remote files exists (e.g. non-archive structures)
     */
    private EnrichedMotifDefinition defaultMapping(MotifDefinition motifDefinition) {
        Structure structure = structureDataProvider.readSome(motifDefinition.getStructureIdentifier());
        List<Map<LabelAtomId, float[]>> residues = structure.manifestResidues(motifDefinition.getLabelSelections());
        return new EnrichedMotifDefinition(motifDefinition, structure, residues);
    }

    @Override
    public Set<EnrichedMotifDefinition> getEnrichedMotifDefinitions() {
        if (dirty.get()) sync(this::defaultMapping);
        return enrichedMotifDefinitions;
    }

    @Override
    public Set<EnrichedMotifDefinition> enrichMotifDefinitions(Function<MotifDefinition, EnrichedMotifDefinition> mapper) {
        return motifDefinitions.stream()
                .map(mapper)
                .collect(Collectors.toSet());
    }

    @Override
    public Function<MotifDefinition, EnrichedMotifDefinition> getDefaultEnricher() {
        return this::defaultMapping;
    }

    private synchronized void sync(Function<MotifDefinition, EnrichedMotifDefinition> mapper) {
        if (!dirty.get()) return;
        long start = System.nanoTime();
        enrichedMotifDefinitions = enrichMotifDefinitions(mapper);
        logger.info("Synchronized enriched motifs in {} ms", (System.nanoTime() - start) * 0.001 * 0.001);
        dirty.set(false);
    }
}
