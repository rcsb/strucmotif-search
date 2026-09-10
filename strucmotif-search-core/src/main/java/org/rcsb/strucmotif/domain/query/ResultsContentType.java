package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.domain.structure.EntryIds;

import java.util.function.Predicate;

/**
 * Controls the set of allowed targets, effectively providing high-level control to find exclusively PDB structures, or
 * exclusively computed structure models.
 */
public enum ResultsContentType implements Predicate<String> {
    /**
     * Return only PDB-entries.
     */
    EXPERIMENTAL(EntryIds::isPdbId),
    /**
     * Return only computed structure models.
     */
    COMPUTATIONAL(EntryIds::isComputedModelId);

    private final Predicate<String> condition;

    ResultsContentType(Predicate<String> condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(String s) {
        return condition.test(s);
    }
}
