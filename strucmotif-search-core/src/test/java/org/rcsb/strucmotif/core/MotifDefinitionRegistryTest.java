package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MotifDefinitionRegistryTest {
    private MotifDefinitionRegistryImpl registry;

    @BeforeEach
    public void init() {
        this.registry = new MotifDefinitionRegistryImpl();
        registry.init();
    }

    @Test
    public void afterRegistryInitialized_thenHardCodedAndDynamicMotifsAvailable() {
        List<MotifDefinition> motifDefinitions = registry.getMotifDefinitions();
        assertTrue(motifDefinitions.contains(MotifDefinition.CHCH));
        assertTrue(motifDefinitions.contains(MotifDefinition.CHH));
        assertTrue(motifDefinitions.contains(MotifDefinition.GGGG));
        assertTrue(motifDefinitions.contains(MotifDefinition.HDS));
        assertTrue(motifDefinitions.contains(MotifDefinition.KDDDE));
        assertTrue(motifDefinitions.contains(MotifDefinition.KDEEH));
        assertTrue(motifDefinitions.contains(MotifDefinition.KDEEH_EXCHANGES));
        assertTrue(motifDefinitions.stream().anyMatch(m -> m.getTitle().startsWith("NOS")), "NOS parsed from JSON missing");
        assertTrue(motifDefinitions.size() > 7);
    }

    @Test
    public void whenRegistryCleared_thenIsEmpty() {
        registry.getMotifDefinitions().clear();
        assertTrue(registry.getMotifDefinitions().isEmpty());
    }
}