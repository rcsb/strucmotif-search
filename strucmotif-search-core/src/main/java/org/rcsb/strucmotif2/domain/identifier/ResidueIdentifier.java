package org.rcsb.strucmotif2.domain.identifier;

import org.rcsb.strucmotif2.domain.structure.PolymerType;
import org.rcsb.strucmotif2.domain.structure.ResidueType;

import java.util.Objects;

/**
 * Uniquely identifies a residue within a chain.
 */
public class ResidueIdentifier {
    private final ResidueType residueType;
    private final int labelSeqId;
    private final int index;

    /**
     * Construct a residue identifier.
     * @param residueType label_comp_id
     * @param labelSeqId label_seq_id
     * @param index index of this residue
     */
    public ResidueIdentifier(String residueType, int labelSeqId, int index) {
        this(ResidueType.ofThreeLetterCode(residueType), labelSeqId, index);
    }

    /**
     * Construct a residue identifier.
     * @param residueType wrapped/normalized label_comp_id
     * @param labelSeqId label_seq_id
     * @param index index of this residue
     */
    public ResidueIdentifier(ResidueType residueType, int labelSeqId, int index) {
        this.residueType = residueType;
        this.labelSeqId = labelSeqId;
        this.index = index;
    }

    /**
     * Enumerated label_comp_id.
     * @return a standardized residue type
     */
    public ResidueType getResidueType() {
        return residueType;
    }

    /**
     * Check if amino acid.
     * @return true if polymer_type is amino_acid
     */
    public boolean isAminoAcid() {
        return residueType.getPolymerType() == PolymerType.AMINO_ACID;
    }

    /**
     * Check if nucleotide.
     * @return true if polymer_type is nucleotide.
     */
    public boolean isNucleotide() {
        return residueType.getPolymerType() == PolymerType.NUCLEOTIDE;
    }

    /**
     * label_seq_id of this residue.
     * @return an int
     */
    public int getLabelSeqId() {
        return labelSeqId;
    }

    /**
     * Index position of this residue in the structure.
     * @return an int
     */
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return residueType.getOneLetterCode() + labelSeqId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResidueIdentifier that = (ResidueIdentifier) o;
        return labelSeqId == that.labelSeqId &&
                // ignore type to fix microheterogeneity ('fix' by ignoring 2nd/3rd/... position)
//                residueType == that.residueType &&
                index == that.index;
    }

    @Override
    public int hashCode() {
        // ignore type to fix microheterogeneity ('fix' by ignoring 2nd/3rd/... position)
        return Objects.hash(/*residueType,*/ labelSeqId, index);
    }
}
