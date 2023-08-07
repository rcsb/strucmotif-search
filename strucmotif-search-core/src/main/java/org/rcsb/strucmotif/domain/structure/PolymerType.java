package org.rcsb.strucmotif.domain.structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reports the polymer type for a component. Used to determine the correct constructor during model creation.
 */
public enum PolymerType {
    /**
     * Amino acid chain.
     */
    AMINO_ACID(Set.of("d-peptide linking",
            "d-peptide nh3 amino terminus",
            "d-peptide cooh carboxy terminus",
            "d-gamma-peptide, c-delta linking",
            "d-beta-peptide, c-gamma linking",
            "l-peptide linking",
            "l-peptide nh3 amino terminus",
            "l-peptide cooh carboxy terminus",
            "l-gamma-peptide, c-delta linking",
            "l-beta-peptide, c-gamma linking",
            "peptide linking",
            "peptide-like")),
    /**
     * Nucleotide chain.
     */
    NUCLEOTIDE(Set.of("dna linking",
            "l-dna linking",
            "dna oh 5 prime terminus",
            "dna oh 3 prime terminus",
            "rna linking", "l-rna linking",
            "rna oh 5 prime terminus",
            "rna oh 3 prime terminus")),
    /**
     * Unknown but polymeric.
     */
    UNKNOWN_POLYMER(Collections.emptySet());

    public static final PolymerType[] values = values(); // caching this

    private final Set<String> typeNames;

    PolymerType(Set<String> typeNames) {
        this.typeNames = typeNames;
    }

    /**
     * Access to CCD types that correspond to this polymer type.
     * @return all associated chem_comp.type values
     */
    public Set<String> getTypeNames() {
        return typeNames;
    }

    private static final Map<String, PolymerType> MAPPING = Arrays.stream(PolymerType.values)
            .flatMap(t -> t.getTypeNames().stream().map(n -> Map.entry(n, t)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    /**
     * Map the type of polymeric component to the enum.
     * @param type value to resolve
     * @return the corresponding polymer type
     */
    public static Optional<PolymerType> ofChemCompType(String type) {
        return Optional.ofNullable(MAPPING.get(type.toLowerCase()));
    }
}
