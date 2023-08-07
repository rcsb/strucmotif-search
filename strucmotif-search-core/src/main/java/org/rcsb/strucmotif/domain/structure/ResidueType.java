package org.rcsb.strucmotif.domain.structure;

/**
 * Registry of known residues. Provides mapping between enum representation and one- and three-letter codes as well as
 * polymer type.
 */
public enum ResidueType {
    // amino acids
    /**
     * Alanine
     */
    ALANINE("ALA", "A", PolymerType.AMINO_ACID),
    /**
     * Cysteine
     */
    CYSTEINE("CYS", "C", PolymerType.AMINO_ACID),
    /**
     * Aspartic acid
     */
    ASPARTIC_ACID("ASP", "D", PolymerType.AMINO_ACID),
    /**
     * Glutamic acid
     */
    GLUTAMIC_ACID("GLU", "E", PolymerType.AMINO_ACID),
    /**
     * Phenylalanine
     */
    PHENYLALANINE("PHE", "F", PolymerType.AMINO_ACID), // 5
    /**
     * Glycine
     */
    GLYCINE("GLY", "G", PolymerType.AMINO_ACID),
    /**
     * Histidine
     */
    HISTIDINE("HIS", "H", PolymerType.AMINO_ACID),
    /**
     * Isoleucine
     */
    ISOLEUCINE("ILE", "I", PolymerType.AMINO_ACID),
    /**
     * Lysine
     */
    LYSINE("LYS", "K", PolymerType.AMINO_ACID),
    /**
     * Leucine
     */
    LEUCINE("LEU", "L", PolymerType.AMINO_ACID), // 10
    /**
     * Methionine
     */
    METHIONINE("MET", "M", PolymerType.AMINO_ACID),
    /**
     * Asparagine
     */
    ASPARAGINE("ASN", "N", PolymerType.AMINO_ACID),
    /**
     * Pyrrolysine
     */
    PYRROLYSINE("PYL", "O", PolymerType.AMINO_ACID),
    /**
     * Proline
     */
    PROLINE("PRO", "P", PolymerType.AMINO_ACID),
    /**
     * Glutamine
     */
    GLUTAMINE("GLN", "Q", PolymerType.AMINO_ACID), // 15
    /**
     * Arginine
     */
    ARGININE("ARG", "R", PolymerType.AMINO_ACID),
    /**
     * Serine
     */
    SERINE("SER", "S", PolymerType.AMINO_ACID),
    /**
     * Threonine
     */
    THREONINE("THR", "T", PolymerType.AMINO_ACID),
    /**
     * Selenocysteine
     */
    SELENOCYSTEINE("SEC", "U", PolymerType.AMINO_ACID),
    /**
     * Valine
     */
    VALINE("VAL", "V", PolymerType.AMINO_ACID), // 20
    /**
     * Tryptophan
     */
    TRYPTOPHAN("TRP", "W", PolymerType.AMINO_ACID),
    /**
     * Tyrosine
     */
    TYROSINE("TYR", "Y", PolymerType.AMINO_ACID), // 22

    // DNA
    /**
     * 2'-Deoxyadenosine-5'-monophosphate
     */
    DEOXYADENOSINE("DA", "0", PolymerType.NUCLEOTIDE),
    /**
     * 2'-Deoxycytidine-5'-monophosphate
     */
    DEOXYCYTIDINE("DC", "1", PolymerType.NUCLEOTIDE),
    /**
     * 2'-Deoxyguanosine-5'-monophosphate
     */
    DEOXYGUANOSINE("DG", "2", PolymerType.NUCLEOTIDE), // 25
    /**
     * 2'-Deoxyinosine-5'-monophosphate
     */
    DEOXYINOSINE("DI", "3", PolymerType.NUCLEOTIDE),
    /**
     * 2'-Deoxythymidine-5'-monophosphate
     */
    DEOXYTHYMIDINE("DT", "4", PolymerType.NUCLEOTIDE),
    /**
     * 2'-Deoxyuridine-5'-monophosphate
     */
    DEOXYURIDINE("DU", "5", PolymerType.NUCLEOTIDE),

    // RNA
    /**
     * Adenosine-5'-monophosphate
     */
    ADENOSINE("A", "6", PolymerType.NUCLEOTIDE),
    /**
     * Cytidine-5'-monophosphate
     */
    CYTIDINE("C", "7", PolymerType.NUCLEOTIDE), // 30
    /**
     * Guanosine-5'-monophosphate
     */
    GUANOSINE("G", "8", PolymerType.NUCLEOTIDE),
    /**
     * Inosinic acid
     */
    INOSINIC_ACID("I", "9", PolymerType.NUCLEOTIDE),
    /**
     * Uridine-5'-monophosphate
     */
    URIDINE("U", "B", PolymerType.NUCLEOTIDE), // 33

    /**
     * Some unknown amino acid.
     */
    UNKNOWN_AMINO_ACID("UNK", "J", PolymerType.AMINO_ACID),
    /**
     * Some unknown nucleotide.
     */
    UNKNOWN_NUCLEOTIDE("N", "X", PolymerType.NUCLEOTIDE),
    /**
     * Some unknown, yet polymeric component.
     */
    UNKNOWN_COMPONENT("?", "Z", PolymerType.UNKNOWN_POLYMER); // 36

    /**
     * Cached values of this enum. Don't manipulate this array of things will burn.
     */
    public static final ResidueType[] values = values();

    // used to retrieve the correct entity from the label_comp_id field
    private final String threeLetterCode;
    // used to write human-readable files
    private final String internalCode;
    private final PolymerType polymerType;

    ResidueType(String threeLetterCode, String internalCode, PolymerType polymerType) {
        this.threeLetterCode = threeLetterCode;
        this.internalCode = internalCode;
        this.polymerType = polymerType;
    }

    /**
     * The (up to) three-letter code of this residue. Matches labelCompId.
     * @return a String
     */
    public String getThreeLetterCode() {
        return threeLetterCode;
    }

    /**
     * The internal single-character code of this residue. May be 'artificial'/nonstandard. Used e.g. to name inverted
     * index files.
     * @return a String
     */
    public String getInternalCode() {
        return internalCode;
    }

    /**
     * The polymer type of this residue.
     * @return amino acid or nucleotide
     */
    public PolymerType getPolymerType() {
        return polymerType;
    }
}
