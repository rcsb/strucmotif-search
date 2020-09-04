package org.rcsb.strucmotif.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.AllPurposeReader;
import org.rcsb.strucmotif.io.read.AllPurposeReaderImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.rcsb.strucmotif.Helpers.getOriginalBcif;

public class MotifSearchIntegrationTest {
    private AllPurposeReader allPurposeReader;

    @BeforeEach
    public void init() {
        this.allPurposeReader = new AllPurposeReaderImpl();
    }

    /**
     * A complex query covering all bases: basal functionality, ambiguity among identifiers and position-specific
     * exchanges.
     */
    @Test
    public void whenSearchingForEnolaseSuperfamily_thenFindExchanges() {
        Structure structure = allPurposeReader.readFromInputStream(getOriginalBcif("2mnr"),
                Set.of(new LabelSelection("A", 1, 162), // K
                new LabelSelection("A", 1, 193), // D
                new LabelSelection("A", 1, 219), // E
                new LabelSelection("A", 1, 245), // E
                new LabelSelection("A", 1, 295))); // H

        // TODO mock this
        QueryBuilder.OptionalStepBuilder buildParameters = MotifSearch.newQuery()
                .defineByStructure(structure)
                .backboneDistanceTolerance(1)
                .sideChainDistanceTolerance(1)
                .angleTolerance(1)
                .buildParameters()
                .addPositionSpecificExchange(new LabelSelection("A", 1, 162), Set.of(ResidueType.LYSINE, ResidueType.HISTIDINE))
                .addPositionSpecificExchange(new LabelSelection("A", 1, 245), Set.of(ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE))
                .addPositionSpecificExchange(new LabelSelection("A", 1, 295), Set.of(ResidueType.HISTIDINE, ResidueType.LYSINE));

        MotifSearchResult response = buildParameters.buildQuery().run();

        List<String> observedExchanges = response.getHits()
                .stream()
                .map(Hit::getResidueTypes)
                .map(a -> Arrays.stream(a).map(ResidueType::getOneLetterCode).collect(Collectors.joining("")))
                .filter(identifiers -> !"DEKEH".equals(identifiers))
                .collect(Collectors.toList());

        observedExchanges.stream()
                .distinct()
                .forEach(System.out::println);

        assertFalse(observedExchanges.isEmpty(), "didnt observe exchange");
    }
}
