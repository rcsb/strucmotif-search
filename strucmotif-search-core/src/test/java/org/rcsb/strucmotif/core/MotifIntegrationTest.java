package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.query.MotifContextBuilder;
import org.rcsb.strucmotif.domain.result.MotifHit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.DefaultStructureReader;
import org.rcsb.strucmotif.io.ResidueTypeResolver;
import org.rcsb.strucmotif.io.DefaultResidueTypeResolver;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.DefaultStateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.DefaultStructureIndexProvider;
import org.rcsb.strucmotif.io.StructureReader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

class MotifIntegrationTest {
    private StructureReader structureReader;
    private Set<EnrichedMotifDefinition> motifs;
    private MotifContextBuilder contextBuilder;

    @BeforeEach
    public void init() {
        StrucmotifConfig strucmotifConfig = new StrucmotifConfig();
        NoOperationMotifPruner noOperationMotifPruner = new NoOperationMotifPruner();
        KruskalMotifPruner kruskalMotifPruner = new KruskalMotifPruner();
        ResidueTypeResolver residueTypeResolver = new DefaultResidueTypeResolver(strucmotifConfig);
        this.structureReader = new DefaultStructureReader(residueTypeResolver);
        AlignmentService alignmentService = new QuaternionAlignmentService();

        StructureDataProvider structureDataProvider = Mockito.mock(StructureDataProvider.class);
        when(structureDataProvider.readRenumbered(any())).thenAnswer(invocation -> {
            String structureIdentifier = invocation.getArgument(0, String.class);
            InputStream inputStream = Helpers.getResource("renum/" + structureIdentifier + ".bcif.gz");
            return structureReader.readFromInputStream(inputStream);
        });

        StateRepository stateRepository = new DefaultStateRepository(strucmotifConfig) {
            @Override
            public Set<StructureInformation> selectKnown() {
                InputStream inputStream = Helpers.getResource("known.list");
                return new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .map(line -> line.split(","))
                        .map(this::handleKnownSplit)
                        .collect(Collectors.toSet());
            }
        };

        StructureIndexProvider structureIndexProvider = new DefaultStructureIndexProvider(stateRepository);
        TargetAssembler targetAssembler = new DefaultTargetAssembler(structureIndexProvider);
        StrucmotifRuntime strucmotifRuntime = new DefaultStrucmotifRuntime(targetAssembler, strucmotifConfig, alignmentService);
        this.motifs = new DefaultMotifDefinitionRegistry(structureDataProvider)
                .enrichMotifDefinitions(this::loadMotif)
                .stream()
                .filter(m -> !m.getMotifIdentifier().equals("KDEEH"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.contextBuilder = new MotifContextBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, strucmotifRuntime, strucmotifConfig);
    }

    private EnrichedMotifDefinition loadMotif(MotifDefinition motifDefinition) {
        try {
            Structure structure = structureReader.readFromInputStream(getOriginalBcif(motifDefinition.getStructureIdentifier()));
            List<Map<LabelAtomId, float[]>> residues = motifDefinition.getLabelSelections()
                    .stream()
                    .map(structure::getResidueIndex)
                    .map(structure::manifestResidue)
                    .toList();
            return new EnrichedMotifDefinition(motifDefinition, structure, residues);
        } catch (UncheckedIOException e) {
            throw new RuntimeException("Structure data for all motifs used during tests must be stored in test/resources/orig/ - missing: " + motifDefinition.getStructureIdentifier(), e);
        }
    }

    @Test
    void whenScreening2mnr_thenSuperfamilyMotifFound() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        MotifSearchResult result = contextBuilder.defineByStructureAndAssemblyId(structure, "1")
                // these must be 'enriched' with structure data outside
                .withMotifs(motifs)
                .rmsdCutoff(2.0)
                .buildParameters()
                .buildContext()
                .run();

        assertEquals(8, result.getHits().size());
        for (MotifHit actual : result.getHits()) {
            MotifDefinition expected = MotifDefinition.KDEEH_EXCHANGES;
            assertEquals(expected.getMotifIdentifier(), actual.motifIdentifier());
            assertShallowEquals(expected.getLabelSelections(), actual.labelSelections());
            assertEquals(1, actual.labelSelections().stream().map(LabelSelection::structOperId).distinct().count());
            assertEquals(List.of(ResidueType.LYSINE, ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.HISTIDINE), actual.residueTypes());
            assertTrue(actual.rmsd() < 0.001);
        }
    }

    private void assertShallowEquals(List<LabelSelection> expected, List<LabelSelection> actual) {
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).labelAsymId(), actual.get(i).labelAsymId());
            assertEquals(expected.get(i).labelSeqId(), actual.get(i).labelSeqId());
        }
    }
}
