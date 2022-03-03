package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.query.SpriteContextBuilder;
import org.rcsb.strucmotif.domain.result.SpriteHit;
import org.rcsb.strucmotif.domain.result.SpriteMotifSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.AssemblyInformationProviderImpl;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class SpriteIntegrationTest {
    private StructureReader structureReader;
    private List<EnrichedMotifDefinition> motifs;
    private SpriteContextBuilder queryBuilder;

    @BeforeEach
    public void init() {
        MotifSearchConfig motifSearchConfig = new MotifSearchConfig();
        ThreadPool threadPool = new ThreadPoolImpl(motifSearchConfig);
        NoOperationMotifPruner noOperationMotifPruner = new NoOperationMotifPruner();
        KruskalMotifPruner kruskalMotifPruner = new KruskalMotifPruner();
        this.structureReader = new StructureReaderImpl();
        AlignmentService alignmentService = new QuaternionAlignmentService();

        StructureDataProvider structureDataProvider = Mockito.mock(StructureDataProvider.class);
        when(structureDataProvider.readRenumbered(any())).thenAnswer(invocation -> {
            String structureIdentifier = invocation.getArgument(0, String.class);
            InputStream inputStream = Helpers.getResource("renum/" + structureIdentifier + ".bcif.gz");
            return structureReader.readFromInputStream(inputStream);
        });

        StateRepository stateRepository = new StateRepositoryImpl(motifSearchConfig) {
            @Override
            public Collection<StructureInformation> selectKnown() {
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
        AssemblyInformationProvider assemblyInformationProvider = new AssemblyInformationProviderImpl(stateRepository, motifSearchConfig);
        MotifSearchRuntime motifSearchRuntime = new MotifSearchRuntimeImpl(targetAssembler, threadPool, motifSearchConfig, alignmentService, assemblyInformationProvider);
        this.motifs = new MotifDefinitionRegistryImpl()
                .getMotifDefinitions()
                .stream()
                .filter(m -> !m.getMotifIdentifier().equals("KDEEH"))
                .map(this::loadMotif)
                .collect(Collectors.toList());
        this.queryBuilder = new SpriteContextBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, motifSearchRuntime, motifSearchConfig);
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
    public void run() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        SpriteMotifSearchResult result = queryBuilder.defineByStructure(structure)
                // these must be 'enriched' with structure data outside
                .andMotifs(motifs)
                .buildParameters()
                .buildContext()
                .run();

        assertTrue(result.getHits().size() > 0);
        SpriteHit actual = result.getHits().get(0);
        MotifDefinition expected = MotifDefinition.KDEEH_EXCHANGES;
        assertEquals(expected.getMotifIdentifier(), actual.getMotifIdentifier());
        assertEquals(expected.getLabelSelections(), actual.getLabelSelections());
        assertEquals(List.of(ResidueType.LYSINE, ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.GLUTAMIC_ACID, ResidueType.HISTIDINE), actual.getResidueTypes());
        assertTrue(actual.getRootMeanSquareDeviation() < 0.001);

        // TODO this should operate on the assembly
    }
}
