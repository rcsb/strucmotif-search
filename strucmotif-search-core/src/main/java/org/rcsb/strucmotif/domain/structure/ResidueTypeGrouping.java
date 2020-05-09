package org.rcsb.strucmotif.domain.structure;

import java.util.Set;

/**
 * Some popular ways of grouping residues by type. For now, only amino acids are considered.
 */
public enum ResidueTypeGrouping {
    HYDROPHOBIC(ResidueType.ALANINE, ResidueType.ISOLEUCINE, ResidueType.LEUCINE, ResidueType.METHIONINE, ResidueType.PHENYLALANINE, ResidueType.VALINE, ResidueType.PROLINE, ResidueType.GLYCINE),
    CHARGED(ResidueType.ARGININE, ResidueType.LYSINE, ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
    POLAR(ResidueType.GLUTAMINE, ResidueType.ASPARAGINE, ResidueType.HISTIDINE, ResidueType.SERINE, ResidueType.THREONINE, ResidueType.TYROSINE, ResidueType.CYSTEINE),
    AMPHIPATHIC(ResidueType.TRYPTOPHAN, ResidueType.TYROSINE, ResidueType.METHIONINE),

    /**
     * Gutteridge, A. and Thornton, J. M.: Understanding nature's catalytic toolkit Trends in biochemical sciences,
     * Elsevier, 2005, 30, 622-629.
     */
    GUANIDINIUM(ResidueType.ARGININE),
    AMIDE(ResidueType.ASPARAGINE, ResidueType.GLUTAMINE),
    CARBOXYLATE(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
    THIOL(ResidueType.CYSTEINE),
    HYDROXY(ResidueType.SERINE, ResidueType.THREONINE, ResidueType.TYROSINE),
    IMIDAZOLE(ResidueType.HISTIDINE),
    AMINO(ResidueType.LYSINE),
    GUTTERIDGE_NEGATIVE(ResidueType.ALANINE, ResidueType.GLYCINE, ResidueType.ISOLEUCINE, ResidueType.LEUCINE, ResidueType.PHENYLALANINE, ResidueType.PROLINE, ResidueType.TRYPTOPHAN, ResidueType.VALINE),

    AROMATIC(ResidueType.PHENYLALANINE, ResidueType.TYROSINE, ResidueType.TRYPTOPHAN),
    NEGATIVE(ResidueType.ASPARTIC_ACID, ResidueType.GLUTAMIC_ACID),
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
