package org.rcsb.strucmotif.domain.query;

import java.util.function.Predicate;

/**
 * Controls the set of allowed targets, effectively providing high-level control to find exclusively PDB structures, or
 * exclusively computed structure models.
 */
public enum StructureDeterminationMethodology implements Predicate<String> {
    /**
     * Return only PDB-entries.
     */
    EXPERIMENTAL(s -> s.matches(Constants.PDB_REGEX)),
    /**
     * Return only computed structure models.
     */
    COMPUTATIONAL(s -> !s.matches(Constants.PDB_REGEX)),
    // TODO impl INTEGRATIVE
    /**
     * Return all hits, regardless of provenance.
     */
    ALL(s -> true);

    private static class Constants {
        private static final String PDB_REGEX = "^[0-9][a-zA-Z0-9]{3}$";
    }

    private final Predicate<String> condition;

    StructureDeterminationMethodology(Predicate<String> condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(String s) {
        return condition.test(s);
    }
}
