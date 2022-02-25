package org.rcsb.strucmotif.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MotifDefinitionRegistryImpl implements MotifDefinitionRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MotifDefinitionRegistryImpl.class);
    private final List<MotifDefinition> motifDefinitions;

    // Lazy initialization if no JSON is desired
    static class GsonHolder {
        static final Gson instance = new Gson();
    }

    static class ListTypeHolder {
        static final Type instance = new TypeToken<ArrayList<MotifDefinition>>(){}.getType();
    }

    public MotifDefinitionRegistryImpl() {
        this.motifDefinitions = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
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

        logger.debug("Loading definitions from motifs.json");
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("motifs.json");
        loadMotifDefinitions(inputStream);

        logger.info("{} motifs registered", motifDefinitions.size());
    }

    @Override
    public List<MotifDefinition> getMotifDefinitions() {
        return motifDefinitions;
    }

    @Override
    public void loadMotifDefinitions(InputStream inputStream) {
        List<MotifDefinition> motifs = GsonHolder.instance.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), ListTypeHolder.instance);
        motifDefinitions.addAll(motifs);
    }
}
