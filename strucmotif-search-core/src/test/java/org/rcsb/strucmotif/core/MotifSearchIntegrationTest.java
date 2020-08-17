package org.rcsb.strucmotif.core;

import junit.framework.TestCase;
import org.junit.Test;
import org.rcsb.strucmotif.MockMotifSearch;
import org.rcsb.strucmotif.Motifs;
import org.rcsb.strucmotif.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.result.Hit;
import org.rcsb.strucmotif.domain.result.MotifSearchResult;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MotifSearchIntegrationTest {
    /**
     * A complex query covering all bases: basal functionality, ambiguity among identifiers and position-specific
     * exchanges.
     */
    @Test
    public void searchForSuperfamilyTemplateWithExchanges() {
        QueryBuilder.OptionalStepBuilder buildParameters = MockMotifSearch.newQuery()
                .defineByStructure(Motifs.KDEEH_EXCHANGES.getStructure())
                .backboneDistanceTolerance(1)
                .sideChainDistanceTolerance(1)
                .angleTolerance(1)
                .buildParameters();

        // register exchanges (if any)
        for (PositionSpecificExchange positionSpecificExchange : Motifs.KDEEH_EXCHANGES.getPositionSpecificExchanges()) {
            buildParameters.addPositionSpecificExchange(positionSpecificExchange.getLabelSelection(), positionSpecificExchange.getResidueTypes());
        }

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

        TestCase.assertFalse("didnt observe exchange", observedExchanges.isEmpty());
    }
}
