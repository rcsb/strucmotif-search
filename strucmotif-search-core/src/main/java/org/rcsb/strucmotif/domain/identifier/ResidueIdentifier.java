package org.rcsb.strucmotif.domain.identifier;

import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Objects;

/**
 * Uniquely identifies a residue within a chain.
 */
public class ResidueIdentifier {
    private final ResidueType residueType;
    private final int labelSeqId;
    private final int index;

    public ResidueIdentifier(String residueType, int labelSeqId, int index) {
        this(ResidueType.ofThreeLetterCode(residueType), labelSeqId, index);
    }

    public ResidueIdentifier(ResidueType residueType, int labelSeqId, int index) {
        this.residueType = residueType;
        this.labelSeqId = labelSeqId;
        this.index = index;
    }

    public ResidueType getResidueType() {
        return residueType;
    }

    public boolean isAminoAcid() {
        return residueType.getPolymerType() == PolymerType.AMINO_ACID;
    }

    public boolean isNucleotide() {
        return residueType.getPolymerType() == PolymerType.NUCLEOTIDE;
    }

    public int getLabelSeqId() {
        return labelSeqId;
    }

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
