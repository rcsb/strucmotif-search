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
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.AssemblyInformationProviderImpl;
import org.rcsb.strucmotif.io.ResidueTypeResolver;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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
        ThreadPool threadPool = new ThreadPoolImpl(strucmotifConfig);
        NoOperationMotifPruner noOperationMotifPruner = new NoOperationMotifPruner();
        KruskalMotifPruner kruskalMotifPruner = new KruskalMotifPruner();
        ResidueTypeResolver residueTypeResolver = new ResidueTypeResolverImpl(strucmotifConfig);
        this.structureReader = new StructureReaderImpl(residueTypeResolver);
        AlignmentService alignmentService = new QuaternionAlignmentService();

        StructureDataProvider structureDataProvider = Mockito.mock(StructureDataProvider.class);
        when(structureDataProvider.readRenumbered(any())).thenAnswer(invocation -> {
            String structureIdentifier = invocation.getArgument(0, String.class);
            InputStream inputStream = Helpers.getResource("renum/" + structureIdentifier + ".bcif.gz");
            return structureReader.readFromInputStream(inputStream);
        });

        StateRepository stateRepository = new StateRepositoryImpl(strucmotifConfig) {
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

        StructureIndexProvider structureIndexProvider = new StructureIndexProviderImpl(stateRepository);
        TargetAssembler targetAssembler = new TargetAssemblerImpl(threadPool, structureIndexProvider);
        AssemblyInformationProvider assemblyInformationProvider = new AssemblyInformationProviderImpl(stateRepository);
        StrucmotifRuntime strucmotifRuntime = new StrucmotifRuntimeImpl(targetAssembler, threadPool, strucmotifConfig, alignmentService, assemblyInformationProvider);
        this.motifs = new MotifDefinitionRegistryImpl(structureDataProvider)
                .enrichMotifDefinitions(this::loadMotif)
                .stream()
                .filter(m -> !m.getMotifIdentifier().equals("KDEEH"))
                .collect(Collectors.toSet());
        this.contextBuilder = new MotifContextBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, strucmotifRuntime, strucmotifConfig);
    }

    private EnrichedMotifDefinition loadMotif(MotifDefinition motifDefinition) {
        try {
            Structure structure = structureReader.readFromInputStream(getOriginalBcif(motifDefinition.getStructureIdentifier()));
            List<Map<LabelAtomId, float[]>> residues = structure.manifestResidues(motifDefinition.getLabelSelections());
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
                .buildParameters()
                .buildContext()
                .run();

        assertTrue(result.getHits().size() > 0);
        MotifHit actual = result.getHits().get(0);
        MotifDefinition expected = MotifDefinition.KDEEH_EXCHANGES;
        assertEquals(expected.getMotifIdentifier(), actual.getMotifIdentifier());
        assertEquals(expected.getLabelSelections(), actual.getLabelSelections());
        assertEquals(List.of(ResidueType.LYSINE, ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.HISTIDINE), actual.getResidueTypes());
        assertTrue(actual.getRootMeanSquareDeviation() < 0.001);
    }
}
