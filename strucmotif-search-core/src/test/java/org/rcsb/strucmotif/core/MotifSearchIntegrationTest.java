package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.AssemblyInformationProviderImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureReaderImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class MotifSearchIntegrationTest {
    private StructureReader structureReader;
    private QueryBuilder queryBuilder;

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
                        .getResourceAsStream("index/" + residuePairDescriptor + ".msg.gz");
                if (inputStream == null) {
                    throw new IOException();
                }
                return new GZIPInputStream(inputStream);
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
        TargetAssembler targetAssembler = new TargetAssemblerImpl(invertedIndex, threadPool, structureIndexProvider);
        AssemblyInformationProvider assemblyInformationProvider = new AssemblyInformationProviderImpl(stateRepository);
        MotifSearchRuntime motifSearchRuntime = new MotifSearchRuntimeImpl(targetAssembler, threadPool, motifSearchConfig, alignmentService, structureDataProvider, structureIndexProvider, assemblyInformationProvider);
        this.queryBuilder = new QueryBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, motifSearchRuntime, motifSearchConfig);
    }

    @Test
    public void whenResidueSelectionOutOfBounds1_thenThrowIllegalQueryException() {
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> {
            Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
            List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", -62),
                    new LabelSelection("A", "1", -245),
                    new LabelSelection("A", "1", -295));
            queryBuilder.defineByStructureAndSelection(structure, labelSelections).buildParameters().buildQuery().run();
        });
    }

    @Test
    public void whenResidueSelectionOutOfBounds2_thenThrowIllegalQueryException() {
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> {
            Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
            List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 1062),
                    new LabelSelection("A", "1", 10245),
                    new LabelSelection("A", "1", 10295));
            queryBuilder.defineByStructureAndSelection(structure, labelSelections).buildParameters().buildQuery().run();
        });
    }

    @Test
    public void whenFailLateWithMalformedQuery_thenThrowIllegalQueryException() {
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> {
            // this will pass initial checks and fail later in the computation
            Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
            List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 62), // K
                            new LabelSelection("A", "1", 245), // E
                            new LabelSelection("A", "1", 295)); // H
            queryBuilder.defineByStructureAndSelection(structure, labelSelections).buildParameters().buildQuery().run();
        });
    }

    /**
     * A complex query covering all bases: basal functionality, ambiguity among identifiers and position-specific
     * exchanges.
     */
    @Test
    public void whenSearchingForEnolaseSuperfamily_thenFindExchanges() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 162), // K
                        new LabelSelection("A", "1", 193), // D
                        new LabelSelection("A", "1", 219), // E
                        new LabelSelection("A", "1", 245), // E
                        new LabelSelection("A", "1", 295)); // H

        QueryBuilder.OptionalStepBuilder buildParameters = queryBuilder.defineByStructureAndSelection(structure, labelSelections)
                .backboneDistanceTolerance(1)
                .sideChainDistanceTolerance(1)
                .angleTolerance(1)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", "1", 162), Set.of(ResidueType.LYSINE, ResidueType.HISTIDINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 245), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 295), Set.of(ResidueType.HISTIDINE, ResidueType.LYSINE));

        MotifSearchResult response = buildParameters.buildQuery().run();

        assertEquals(422, response.getHits().size());

        List<String> observedExchanges = response.getHits()
                .stream()
                .map(Hit::getResidueTypes)
                .map(a -> a.stream().map(ResidueType::getOneLetterCode).collect(Collectors.joining("")))
                .filter(identifiers -> !"DEKEH".equals(identifiers))
                .collect(Collectors.toList());

        assertFalse(observedExchanges.isEmpty(), "didn't observe exchange");

        List<String> observedAssemblies = response.getHits()
                .stream()
                .map(hit -> hit.getStructureIdentifier() + "_" + hit.getAssemblyIdentifier())
                .filter(assemblyIdentifier -> !assemblyIdentifier.contains("_1"))
                .collect(Collectors.toList());

        assertFalse(observedAssemblies.isEmpty(), "didn't observe assemblies");

        List<List<Integer>> swaps = response.getHits()
                .stream()
                .map(hit -> hit.getLabelSelections().stream().map(LabelSelection::getLabelSeqId).collect(Collectors.toList()))
                // check if some elements are unordered
                .filter(list -> IntStream.range(1, list.size())
                        .map(index -> list.get(index - 1).compareTo(list.get(index)))
                        .anyMatch(order -> order > 0))
                .collect(Collectors.toList());

        assertFalse(swaps.isEmpty(), "didn't observe swaps");

        // print all results
        assertTrue(response.getHits().stream()
                .map(Hit::getRootMeanSquareDeviation)
                .anyMatch(s -> s < 0.5), "no low-RMSD hits observed");
    }

    /**
     * A query defined by non-identity struct_oper_ids.
     */
    @Test
    public void whenAssemblyAndAllowingTransforms_thenReturnFullResidueGraph() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4oog"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("C", "1", 42),
                        new LabelSelection("C", "1", 45),
                        new LabelSelection("C", "1", 49),
                        new LabelSelection("C", "2", 42),
                        new LabelSelection("C", "2", 45),
                        new LabelSelection("C", "2", 49));

        MotifSearchQuery motifSearchQuery = queryBuilder.defineByStructureAndSelection(structure, labelSelections).buildParameters().buildQuery();
        assertEquals(6, motifSearchQuery.getQueryStructure().getResidues().size(), "not all residues present");
    }
}
