package org.rcsb.strucmotif.domain.structure;

import java.util.Set;

/**
 * Some popular ways of grouping residues by type. For now, only amino acids are considered.
 */
public enum ResidueTypeGrouping {
    /**
     * Hydrophobic amino acids.
     */
    HYDROPHOBIC(ResidueType.ALANINE, ResidueType.ISOLEUCINE, ResidueType.LEUCINE, ResidueType.METHIONINE, ResidueType.PHENYLALANINE, ResidueType.VALINE, ResidueType.PROLINE, ResidueType.GLYCINE),
    /**
     * Charged amino acids.
     */
    CHARGED(ResidueType.ARGININE, ResidueType.LYSINE, ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
    /**
     * Polar amino acids.
     */
    POLAR(ResidueType.GLUTAMINE, ResidueType.ASPARAGINE, ResidueType.HISTIDINE, ResidueType.SERINE, ResidueType.THREONINE, ResidueType.TYROSINE, ResidueType.CYSTEINE),
    /**
     * Amphipathic amino acids.
     */
    AMPHIPATHIC(ResidueType.TRYPTOPHAN, ResidueType.TYROSINE, ResidueType.METHIONINE),

    // Gutteridge, A. and Thornton, J. M.: Understanding nature's catalytic toolkit Trends in biochemical sciences,
    // Elsevier, 2005, 30, 622-629.
    /**
     * Guanidinium groups according to Gutteridge et al.
     */
    GUTTERIDGE_GUANIDINIUM(ResidueType.ARGININE),
    /**
     * Amide groups according to Gutteridge et al.
     */
    GUTTERIDGE_AMIDE(ResidueType.ASPARAGINE, ResidueType.GLUTAMINE),
    /**
     * Carboxylate groups according to Gutteridge et al.
     */
    GUTTERIDGE_CARBOXYLATE(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
    /**
     * Thiol groups according to Gutteridge et al.
     */
    GUTTERIDGE_THIOL(ResidueType.CYSTEINE),
    /**
     * Hydroxy groups according to Gutteridge et al.
     */
    GUTTERIDGE_HYDROXY(ResidueType.SERINE, ResidueType.THREONINE, ResidueType.TYROSINE),
    /**
     * Imidazole groups according to Gutteridge et al.
     */
    GUTTERIDGE_IMIDAZOLE(ResidueType.HISTIDINE),
    /**
     * Amino groups according to Gutteridge et al.
     */
    GUTTERIDGE_AMINO(ResidueType.LYSINE),
    /**
     * Negative groups according to Gutteridge et al.
     */
    GUTTERIDGE_NEGATIVE(ResidueType.ALANINE, ResidueType.GLYCINE, ResidueType.ISOLEUCINE, ResidueType.LEUCINE, ResidueType.PHENYLALANINE, ResidueType.PROLINE, ResidueType.TRYPTOPHAN, ResidueType.VALINE),

    /**
     * Aromatic amino acids.
     */
    AROMATIC(ResidueType.PHENYLALANINE, ResidueType.TYROSINE, ResidueType.TRYPTOPHAN),
    /**
     * Negatively charged amino acids.
     */
    NEGATIVE(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
    /**
     * Positively charged amino acids.
     */
    POSITIVE(ResidueType.LYSINE, ResidueType.ARGININE, ResidueType.HISTIDINE);

    private final Set<ResidueType> residueTypes;

    ResidueTypeGrouping(ResidueType... residueTypes) {
        this.residueTypes = Set.of(residueTypes);
    }

    /**
     * All residue types associated to this grouping.
     * @return a collection of types
     */
    public Set<ResidueType> getResidueTypes() {
        return residueTypes;
    }
}
