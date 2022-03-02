package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.SpriteContextBuilder;
import org.rcsb.strucmotif.domain.result.SpriteMotifSearchResult;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.AssemblyInformationProviderImpl;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class SpriteIntegrationTest {
    private StructureReader structureReader;
    private SpriteContextBuilder queryBuilder;

    @BeforeEach
    public void init() {
        MotifSearchConfig motifSearchConfig = new MotifSearchConfig();
        ThreadPool threadPool = new ThreadPoolImpl(motifSearchConfig);
        NoOperationMotifPruner noOperationMotifPruner = new NoOperationMotifPruner(motifSearchConfig);
        KruskalMotifPruner kruskalMotifPruner = new KruskalMotifPruner(motifSearchConfig);
        this.structureReader = new StructureReaderImpl();
        AlignmentService alignmentService = new QuaternionAlignmentService();

        InvertedIndexImpl invertedIndex = new InvertedIndexImpl(motifSearchConfig) {
            @Override
            protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
                // null is okay here
                InputStream inputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("index/" + residuePairDescriptor + ".colf");
                if (inputStream == null) {
                    throw new IOException();
                }
                return inputStream;
            }
        };

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
        MotifDefinitionRegistry motifDefinitionRegistry = new MotifDefinitionRegistryImpl();
        MotifSearchRuntime motifSearchRuntime = new MotifSearchRuntimeImpl(targetAssembler, threadPool, motifSearchConfig, alignmentService, structureDataProvider, structureIndexProvider, assemblyInformationProvider, motifDefinitionRegistry);
        this.queryBuilder = new SpriteContextBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, motifSearchRuntime, motifSearchConfig);
    }

    @Test
    public void run() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        SpriteMotifSearchResult result = queryBuilder.defineByStructure(structure)
                .buildParameters()
                .buildContext()
                .run();

        assertTrue(result.getHits().size() > 0);
    }
}
