package org.rcsb.strucmotif.domain.identifier;

import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Objects;

/**
 * Uniquely identifies a residue within a chain.
 */
public class ResidueIdentifier {
    private final ResidueType residueType;
    private final int authSeqId;
    private final int index;

    public ResidueIdentifier(String residueType, int authSeqId, int index) {
        this(ResidueType.ofThreeLetterCode(residueType), authSeqId, index);
    }

    public ResidueIdentifier(ResidueType residueType, int authSeqId, int index) {
        this.residueType = residueType;
        this.authSeqId = authSeqId;
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

    public int getAuthSeqId() {
        return authSeqId;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return residueType.getOneLetterCode() + authSeqId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResidueIdentifier that = (ResidueIdentifier) o;
        return authSeqId == that.authSeqId &&
                // ignore type to fix microheterogeneity ('fix' by ignoring 2nd/3rd/... position)
//                residueType == that.residueType &&
                index == that.index;
    }

    @Override
    public int hashCode() {
        // ignore type to fix microheterogeneity ('fix' by ignoring 2nd/3rd/... position)
        return Objects.hash(/*residueType,*/ authSeqId, index);
    }
}
