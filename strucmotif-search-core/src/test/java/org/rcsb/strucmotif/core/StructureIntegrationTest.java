package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.ffindex.FileBundleIO;
import org.rcsb.ffindex.ReadableFileBundle;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.align.AlignmentResult;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.bucket.InvertedIndexBucket;
import org.rcsb.strucmotif.domain.query.ResultsContentType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.StructureQuery;
import org.rcsb.strucmotif.domain.query.StructureContextBuilder;
import org.rcsb.strucmotif.domain.result.StructureHit;
import org.rcsb.strucmotif.domain.result.StructureSearchResult;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.rcsb.strucmotif.io.AssemblyInformationProvider;
import org.rcsb.strucmotif.io.AssemblyInformationProviderImpl;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.ResidueTypeResolverImpl;
import org.rcsb.strucmotif.io.StateRepository;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.rcsb.strucmotif.io.StructureIndexProviderImpl;
import org.rcsb.strucmotif.io.StructureReader;
import org.rcsb.strucmotif.io.InvertedIndexImpl;
import org.rcsb.strucmotif.io.StateRepositoryImpl;
import org.rcsb.strucmotif.io.StructureReaderImpl;
import org.rcsb.strucmotif.io.codec.ColferCodec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

class StructureIntegrationTest {
    private StrucmotifConfig strucmotifConfig;
    private StructureReader structureReader;
    private StructureContextBuilder contextBuilder;

    @BeforeEach
    public void init() throws IOException {
        this.strucmotifConfig = new StrucmotifConfig();
        ThreadPool threadPool = new DefaultThreadPool(strucmotifConfig);
        NoOperationMotifPruner noOperationMotifPruner = new NoOperationMotifPruner();
        KruskalMotifPruner kruskalMotifPruner = new KruskalMotifPruner();
        this.structureReader = new StructureReaderImpl(new ResidueTypeResolverImpl(strucmotifConfig));
        AlignmentService alignmentService = new QuaternionAlignmentService();

        ReadableFileBundle fileBundle = FileBundleIO.openBundle(Helpers.getResourceAsPath("index.data"), Helpers.getResourceAsPath("index.ffindex")).inReadOnlyMode();
        ColferCodec bucketCodec = new ColferCodec();
        InvertedIndex invertedIndex = new InvertedIndexImpl(threadPool, strucmotifConfig) {
            @Override
            public InvertedIndexBucket select(ResiduePairDescriptor residuePairDescriptor) {
                String filename = residuePairDescriptor.toString().substring(0, 2) + "/" + residuePairDescriptor + ".colf";
                if (!fileBundle.containsFile(filename)) {
                    return InvertedIndexBucket.EMPTY_BUCKET;
                }

                try {
                    return bucketCodec.decode(fileBundle.readFile(filename));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };

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
        TargetAssembler targetAssembler = new DefaultTargetAssembler(threadPool, structureIndexProvider);
        AssemblyInformationProvider assemblyInformationProvider = new AssemblyInformationProviderImpl(stateRepository);
        StrucmotifRuntime strucmotifRuntime = new DefaultStrucmotifRuntime(targetAssembler, threadPool, strucmotifConfig, alignmentService, assemblyInformationProvider);
        this.contextBuilder = new StructureContextBuilder(structureIndexProvider, structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, strucmotifRuntime, strucmotifConfig, invertedIndex);
    }

    @Test
    void whenResidueSelectionOutOfBounds1_thenThrowIllegalQueryException() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", -62),
                new LabelSelection("A", "1", -245),
                new LabelSelection("A", "1", -295));
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters().buildContext().run());
    }

    @Test
    void whenResidueSelectionOutOfBounds2_thenThrowIllegalQueryException() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 1062),
                new LabelSelection("A", "1", 10245),
                new LabelSelection("A", "1", 10295));
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters().buildContext().run());
    }

    @Test
    void whenFailLateWithMalformedQuery_thenThrowIllegalQueryException() {
        // this will pass initial checks and fail later in the computation
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 62), // K
                new LabelSelection("A", "1", 245), // E
                new LabelSelection("A", "1", 295)); // H
        Assertions.assertThrows(IllegalQueryDefinitionException.class, () -> contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters().buildContext().run());
    }

    /**
     * A complex query covering all bases: basal functionality, ambiguity among identifiers and position-specific
     * exchanges.
     */
    @Test
    void whenSearchingForEnolaseSuperfamily_thenFindExchanges() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 162), // K
                        new LabelSelection("A", "1", 193), // D
                        new LabelSelection("A", "1", 219), // E
                        new LabelSelection("A", "1", 245), // E
                        new LabelSelection("A", "1", 295)); // H

        StructureContextBuilder.OptionalBuilderStep buildParameters = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .backboneDistanceTolerance(1)
                .sideChainDistanceTolerance(1)
                .angleTolerance(1)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", "1", 162), Set.of(ResidueType.LYSINE, ResidueType.HISTIDINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 245), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 295), Set.of(ResidueType.HISTIDINE, ResidueType.LYSINE));

        StructureSearchResult response = buildParameters.buildContext().run();

        assertEquals(422, response.getHits().size());

        List<String> observedExchanges = response.getHits()
                .stream()
                .map(StructureHit::getResidueTypes)
                .map(a -> a.stream().map(ResidueType::getInternalCode).collect(Collectors.joining("")))
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
                .map(StructureHit::getRootMeanSquareDeviation)
                .anyMatch(s -> s < 0.5), "no low-RMSD hits observed");
    }

    /**
     * This one is based on HELP-18632 & HELP-18634 and ensures that modified amino acids get resolved in a reasonable
     * fashion.
     */
    @Test
    void whenSearchingForModifiedAminoAcids_thenResolvedCorrectly() {
        Structure forwardStructure = structureReader.readFromInputStream(getOriginalBcif("6tne"));
        List<LabelSelection> forwardLabelSelections = List.of(new LabelSelection("A", "1", 21), // E
                new LabelSelection("A", "1", 22), // D
                new LabelSelection("A", "1", 67), // D
//                new LabelSelection("A", "1", 95), // S // TODO would be nice to support this natively somehow, CB distance is 2
                new LabelSelection("A", "1", 117)); // K

        Structure backwardStructure = structureReader.readFromInputStream(getOriginalBcif("1zdm"));
        List<LabelSelection> backwardLabelSelections = List.of(new LabelSelection("A", "1", 12), // D
                new LabelSelection("A", "1", 13), // D
                new LabelSelection("A", "1", 57), // BFD -> D
//                new LabelSelection("A", "1", 87), // T
                new LabelSelection("A", "1", 109)); // K

        Collection<String> structures = Set.of("6TNE", "1ZDM");

        List<Map<LabelAtomId, float[]>> forwardResidues = forwardStructure.manifestResidues(forwardLabelSelections);
        List<Map<LabelAtomId, float[]>> backwardResidues = backwardStructure.manifestResidues(backwardLabelSelections);
        AlignmentResult align = new QuaternionAlignmentService().align(forwardResidues, backwardResidues, AtomPairingScheme.ALL);
        assertEquals(0.57, align.getRootMeanSquareDeviation(), Helpers.RELAXED_DELTA, "the motifs should align reasonable well");

        StructureSearchResult forwardResult = contextBuilder.defineByStructureAndSelection(forwardStructure, forwardLabelSelections)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", "1", 21), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID))
                .allowedStructures(structures)
                .buildContext()
                .run();
        assertEquals(3, forwardResult.getHits().size());
        assertTrue(forwardResult.getHits().stream().anyMatch(h -> h.getStructureIdentifier().equals("1ZDM")));

        StructureSearchResult backwardResult = contextBuilder.defineByStructureAndSelection(backwardStructure, backwardLabelSelections)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", "1", 12), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID))
                .allowedStructures(structures)
                .buildContext()
                .run();
        assertEquals(backwardResult.getHits().size(), backwardResult.getHits().stream().map(h -> h.getStructureIdentifier() + h.getLabelSelections()).distinct().count(), "there are duplicate hits");
        assertEquals(3, backwardResult.getHits().size());
        assertTrue(backwardResult.getHits().stream().anyMatch(h -> h.getStructureIdentifier().equals("6TNE")));
    }

    /**
     * A query defined by non-identity struct_oper_ids.
     */
    @Test
    void whenAssemblyAndAllowingTransforms_thenReturnFullResidueGraph() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("4oog"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("C", "1", 42),
                        new LabelSelection("C", "1", 45),
                        new LabelSelection("C", "1", 49),
                        new LabelSelection("C", "2", 42),
                        new LabelSelection("C", "2", 45),
                        new LabelSelection("C", "2", 49));

        StructureQuery structureQuery = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .buildContext()
                .getQuery();
        assertEquals(6, structureQuery.getQueryStructure().getResidues().size(), "not all residues present");
    }

    /**
     * Test subset queries for PDB entries, models, or both.
     */
    @Test
    void whenSearchingForSubset_thenCriteriaHonored() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 162), // K
                new LabelSelection("A", "1", 193), // D
                new LabelSelection("A", "1", 219), // E
                new LabelSelection("A", "1", 245), // E
                new LabelSelection("A", "1", 295)); // H

        int fullQueryCount = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .resultsContentType(ResultsContentType.EXPERIMENTAL, ResultsContentType.COMPUTATIONAL)
                .buildContext()
                .run()
                .getHits()
                .size();
        assertTrue(fullQueryCount > 0);

        int modelQueryCount = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .resultsContentType(ResultsContentType.COMPUTATIONAL)
                .buildContext()
                .run()
                .getHits()
                .size();
        assertEquals(0, modelQueryCount);

        int pdbQueryCount = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .resultsContentType(ResultsContentType.EXPERIMENTAL)
                .buildContext()
                .run()
                .getHits()
                .size();
        assertEquals(fullQueryCount, pdbQueryCount);
    }

    @Test
    void whenDanglingResidue_thenExceptionThrown() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 162), // K
                new LabelSelection("A", "1", 193), // D
                new LabelSelection("A", "1", 219), // E
                new LabelSelection("A", "1", 245), // E
                new LabelSelection("A", "1", 295), // H
                new LabelSelection("A", "1", 43)); // V

        StructureContextBuilder.OptionalBuilderStep step = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .resultsContentType(ResultsContentType.EXPERIMENTAL, ResultsContentType.COMPUTATIONAL);
        assertThrows(IllegalQueryDefinitionException.class, step::buildContext);
    }

    @Test
    void whenFragmentedMotif_thenExceptionThrown() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"));
        List<LabelSelection> labelSelections = List.of(new LabelSelection("A", "1", 162), // K
                new LabelSelection("A", "1", 193), // D
                new LabelSelection("A", "1", 219), // E
                new LabelSelection("A", "1", 245), // E
                new LabelSelection("A", "1", 295), // H
                new LabelSelection("A", "1", 43),  // V
                new LabelSelection("A", "1", 39)); // T

        StructureContextBuilder.OptionalBuilderStep step = contextBuilder.defineByStructureAndSelection(structure, labelSelections)
                .buildParameters()
                .resultsContentType(ResultsContentType.EXPERIMENTAL, ResultsContentType.COMPUTATIONAL);
        assertThrows(IllegalQueryDefinitionException.class, step::buildContext);
    }
}
