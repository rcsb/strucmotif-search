package org.rcsb.strucmotif.domain.identifier;

import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Objects;

/**
 * Uniquely identifies a residue within a chain. Using index and the assembly id of the chain will uniquely identify a
 * residue in the whole structure.
 */
public class ResidueIdentifier {
    private final ResidueType residueType;
    private final int authSeqId;
    private final String insCode;
    private final int index;

    public ResidueIdentifier(String residueType, int authSeqId, String insCode, int index) {
        this(ResidueType.ofThreeLetterCode(residueType), authSeqId, insCode, index);
    }

    public ResidueIdentifier(ResidueType residueType, int authSeqId, String insCode, int index) {
        this.residueType = residueType;
        this.authSeqId = authSeqId;
        this.insCode = insCode;
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

    public String getInsCode() {
        return insCode;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return residueType.getOneLetterCode() + authSeqId + insCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResidueIdentifier that = (ResidueIdentifier) o;
        return authSeqId == that.authSeqId &&
                index == that.index &&
                // ignore type to fix microheterogeneity ('fix' by ignoring 2nd/3rd/... position)
//                residueType == that.residueType &&
                Objects.equals(insCode, that.insCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(/*residueType,*/ authSeqId, insCode, index);
    }
}
