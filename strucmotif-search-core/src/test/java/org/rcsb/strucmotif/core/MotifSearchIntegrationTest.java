package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rcsb.strucmotif.Helpers;
import org.rcsb.strucmotif.align.AlignmentService;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.result.TransformedHit;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.persistence.FileSystemInvertedIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

@SpringBootTest
public class MotifSearchIntegrationTest {
    @Autowired
    private StructureReader structureReader;
    @Autowired
    private KruskalMotifPruner kruskalMotifPruner;
    @Autowired
    private NoOperationMotifPruner noOperationMotifPruner;
    @Autowired
    private ThreadPool threadPool;
    @Autowired
    private MotifSearchConfig motifSearchConfig;
    @Autowired
    private AlignmentService alignmentService;
    private StructureDataProvider structureDataProvider;
    private QueryBuilder queryBuilder;

    @BeforeEach
    public void init() {
        FileSystemInvertedIndex invertedIndex = new FileSystemInvertedIndex(new MotifSearchConfig()) {
            @Override
            protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
                // null is okay here
                InputStream inputStream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("index/" + residuePairDescriptor.toString() + ".msg");
                if (inputStream == null) {
                    throw new IOException();
                }
                return inputStream;
            }
        };

        structureDataProvider = Mockito.mock(StructureDataProvider.class);
        when(structureDataProvider.readRenumbered(any(), any())).thenAnswer(invocation -> {
            StructureIdentifier structureIdentifier = invocation.getArgument(0, StructureIdentifier.class);
            @SuppressWarnings("unchecked")
            Collection<LabelSelection> selection = (Collection<LabelSelection>) invocation.getArgument(1, Collection.class);

            InputStream inputStream = Helpers.getResource("renum/" + structureIdentifier.getPdbId() + ".bcif.gz");
            return structureReader.readFromInputStream(inputStream, selection);
        });

        TargetAssembler targetAssembler = new TargetAssemblerImpl(invertedIndex, threadPool);
        MotifSearchRuntimeImpl motifSearchRuntime = new MotifSearchRuntimeImpl(targetAssembler, threadPool, motifSearchConfig);
        this.queryBuilder = new QueryBuilder(structureDataProvider, kruskalMotifPruner, noOperationMotifPruner, motifSearchRuntime, motifSearchConfig);
    }

    /**
     * A complex query covering all bases: basal functionality, ambiguity among identifiers and position-specific
     * exchanges.
     */
    @Test
    public void whenSearchingForEnolaseSuperfamily_thenFindExchanges() {
        Structure structure = structureReader.readFromInputStream(getOriginalBcif("2mnr"),
                Set.of(new LabelSelection("A", "1", 162), // K
                        new LabelSelection("A", "1", 193), // D
                        new LabelSelection("A", "1", 219), // E
                        new LabelSelection("A", "1", 245), // E
                        new LabelSelection("A", "1", 295))); // H

        QueryBuilder.OptionalStepBuilder buildParameters = queryBuilder.defineByStructure(structure)
                .backboneDistanceTolerance(1)
                .sideChainDistanceTolerance(1)
                .angleTolerance(1)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", "1", 162), Set.of(ResidueType.LYSINE, ResidueType.HISTIDINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 245), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE))
                .addPositionSpecificExchange(new LabelSelection("A", "1", 295), Set.of(ResidueType.HISTIDINE, ResidueType.LYSINE));

        HitScorer hitScorer = new RootMeanSquareDeviationHitScorer(structure, AtomPairingScheme.ALL, alignmentService, structureDataProvider);
        MotifSearchResult response = buildParameters.buildQuery().run();

        List<String> observedExchanges = response.getHits()
                .stream()
                .map(hitScorer::score)
                .map(TransformedHit::getResidueTypes)
                .map(a -> a.stream().map(ResidueType::getOneLetterCode).collect(Collectors.joining("")))
                .filter(identifiers -> !"DEKEH".equals(identifiers))
                .collect(Collectors.toList());

        assertFalse(observedExchanges.isEmpty(), "didn't observe exchange");
    }
}
