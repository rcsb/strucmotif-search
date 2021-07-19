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
     * Proline
     */
    PROLINE("PRO", "P", PolymerType.AMINO_ACID),
    /**
     * Glutamine
     */
    GLUTAMINE("GLN", "Q", PolymerType.AMINO_ACID),
    /**
     * Arginine
     */
    ARGININE("ARG", "R", PolymerType.AMINO_ACID), // 15
    /**
     * Serine
     */
    SERINE("SER", "S", PolymerType.AMINO_ACID),
    /**
     * Threonine
     */
    THREONINE("THR", "T", PolymerType.AMINO_ACID),
    /**
     * Valine
     */
    VALINE("VAL", "V", PolymerType.AMINO_ACID),
    /**
     * Tryptophan
     */
    TRYPTOPHAN("TRP", "W", PolymerType.AMINO_ACID),
    /**
     * Tyrosine
     */
    TYROSINE("TYR", "Y", PolymerType.AMINO_ACID), // 20

    // nucleotides
    /**
     * Adenosine
     */
    ADENOSINE("A", "0", PolymerType.NUCLEOTIDE),
    /**
     * Cytidine
     */
    CYTIDINE("C", "1", PolymerType.NUCLEOTIDE),
    /**
     * Deoxyadenosine
     */
    DEOXYADENOSINE("DA", "2", PolymerType.NUCLEOTIDE),
    /**
     * Deoxycytidine
     */
    DEOXYCYTIDINE("DC", "3", PolymerType.NUCLEOTIDE),
    /**
     * Deoxyguanosine
     */
    DEOXYGUANOSINE("DG", "4", PolymerType.NUCLEOTIDE), // 25
    /**
     * Guanosine
     */
    GUANOSINE("G", "5", PolymerType.NUCLEOTIDE),
    /**
     * Thymidine
     */
    THYMIDINE("T", "6", PolymerType.NUCLEOTIDE),
    /**
     * Uridine
     */
    URIDINE("U", "7", PolymerType.NUCLEOTIDE),

    // joker categories
//    LIGAND("LIG", "l"),

    // stuff to ignore for hashCode creation
    /**
     * Some unknown amino acid.
     */
    UNKNOWN_AMINO_ACID("UNK", "X", null),
    /**
     * Some unknown nucleotide.
     */
    UNKNOWN_NUCLEOTIDE("N", "Z", null),
    /**
     * Some unknown, yet polymeric component.
     */
    UNKNOWN_COMPONENT("?", "U", null);

    // used to retrieve the correct entity from the label_comp_id field
    private final String threeLetterCode;
    // used to write human-readable files
    private final String oneLetterCode;
    private final PolymerType polymerType;

    ResidueType(String threeLetterCode, String oneLetterCode, PolymerType polymerType) {
        this.threeLetterCode = threeLetterCode;
        this.oneLetterCode = oneLetterCode;
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
     * The one-letter code of this residue. May be 'artificial'/nonstandard. Used e.g. to name inverted index files.
     * @return a String
     */
    public String getOneLetterCode() {
        return oneLetterCode;
    }

    /**
     * The polymer type of this residue.
     * @return amino acid or nucleotide
     */
    public PolymerType getPolymerType() {
        return polymerType;
    }

    /**
     * Maps labelCompId values to the correct {@link ResidueType}.
     * @param labelCompId three-letter code to process
     * @return the matching type - if argument is null or fail to find: return UNKNOWN_AMINO_ACID
     */
    public static ResidueType ofThreeLetterCode(String labelCompId) {
        if (labelCompId == null) {
            return UNKNOWN_COMPONENT;
        }

        switch (labelCompId) {
            case "ALA":
                return ALANINE;
            case "ARG":
                return ARGININE;
            case "ASN":
                return ASPARAGINE;
            case "ASP":
                return ASPARTIC_ACID;
            case "CYS":
                return CYSTEINE;
            case "GLU":
                return GLUTAMIC_ACID;
            case "GLN":
                return GLUTAMINE;
            case "GLY":
                return GLYCINE;
            case "HIS":
                return HISTIDINE;
            case "ILE":
                return ISOLEUCINE;
            case "LEU":
                return LEUCINE;
            case "LYS":
                return LYSINE;
            case "MET":
                return METHIONINE;
            case "PHE":
                return PHENYLALANINE;
            case "PRO":
                return PROLINE;
            case "SER":
                return SERINE;
            case "THR":
                return THREONINE;
            case "TRP":
                return TRYPTOPHAN;
            case "TYR":
                return TYROSINE;
            case "VAL":
                return VALINE;

            case "A":
                return ADENOSINE;
            case "C":
                return CYTIDINE;
            case "DA":
                return DEOXYADENOSINE;
            case "DC":
                return DEOXYCYTIDINE;
            case "DG":
                return DEOXYGUANOSINE;
            case "G":
                return GUANOSINE;
            case "T":
                return THYMIDINE;
            case "U":
                return URIDINE;


            case "UNK":
                return UNKNOWN_AMINO_ACID;
            case "N":
                return UNKNOWN_NUCLEOTIDE;
            default:
                return UNKNOWN_COMPONENT;
        }
    }

    /**
     * Resolve a one-letter-code.
     * @param oneLetterCode the one-letter-code (as defined in this implementation)
     * @return a residue type or null
     */
    public static ResidueType ofOneLetterCode(String oneLetterCode) {
        if (oneLetterCode == null) {
            return UNKNOWN_COMPONENT;
        }

        switch (oneLetterCode) {
            case "A":
                return ALANINE;
            case "R":
                return ARGININE;
            case "N":
                return ASPARAGINE;
            case "D":
                return ASPARTIC_ACID;
            case "C":
                return CYSTEINE;
            case "E":
                return GLUTAMIC_ACID;
            case "Q":
                return GLUTAMINE;
            case "G":
                return GLYCINE;
            case "H":
                return HISTIDINE;
            case "I":
                return ISOLEUCINE;
            case "L":
                return LEUCINE;
            case "K":
                return LYSINE;
            case "M":
                return METHIONINE;
            case "F":
                return PHENYLALANINE;
            case "P":
                return PROLINE;
            case "S":
                return SERINE;
            case "T":
                return THREONINE;
            case "W":
                return TRYPTOPHAN;
            case "Y":
                return TYROSINE;
            case "V":
                return VALINE;

            case "0":
                return ADENOSINE;
            case "1":
                return CYTIDINE;
            case "2":
                return DEOXYADENOSINE;
            case "3":
                return DEOXYCYTIDINE;
            case "4":
                return DEOXYGUANOSINE;
            case "5":
                return GUANOSINE;
            case "6":
                return THYMIDINE;
            case "7":
                return URIDINE;


            case "X":
                return UNKNOWN_AMINO_ACID;
            case "Z":
                return UNKNOWN_NUCLEOTIDE;
            default:
                return UNKNOWN_COMPONENT;
        }
    }
}
