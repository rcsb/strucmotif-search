package org.rcsb.strucmotif.domain.structure;

/**
 * Registry of known components. Provides mapping between enum representation and one- and three-letter codes as well as
 * polymer type.
 */
public enum ResidueType {
    // amino acids
    ALANINE("ALA", "A", PolymerType.AMINO_ACID),
    CYSTEINE("CYS", "C", PolymerType.AMINO_ACID),
    ASPARTIC_ACID("ASP", "D", PolymerType.AMINO_ACID),
    GLUTAMIC_ACID("GLU", "E", PolymerType.AMINO_ACID),
    PHENYLALANINE("PHE", "F", PolymerType.AMINO_ACID), // 5
    GLYCINE("GLY", "G", PolymerType.AMINO_ACID),
    HISTIDINE("HIS", "H", PolymerType.AMINO_ACID),
    ISOLEUCINE("ILE", "I", PolymerType.AMINO_ACID),
    LYSINE("LYS", "K", PolymerType.AMINO_ACID),
    LEUCINE("LEU", "L", PolymerType.AMINO_ACID), // 10
    METHIONINE("MET", "M", PolymerType.AMINO_ACID),
    ASPARAGINE("ASN", "N", PolymerType.AMINO_ACID),
    PROLINE("PRO", "P", PolymerType.AMINO_ACID),
    GLUTAMINE("GLN", "Q", PolymerType.AMINO_ACID),
    ARGININE("ARG", "R", PolymerType.AMINO_ACID), // 15
    SERINE("SER", "S", PolymerType.AMINO_ACID),
    THREONINE("THR", "T", PolymerType.AMINO_ACID),
    VALINE("VAL", "V", PolymerType.AMINO_ACID),
    TRYPTOPHAN("TRP", "W", PolymerType.AMINO_ACID),
    TYROSINE("TYR", "Y", PolymerType.AMINO_ACID), // 20

    // nucleotides
    ADENOSINE("A", "0", PolymerType.NUCLEOTIDE),
    CYTIDINE("C", "1", PolymerType.NUCLEOTIDE),
    DEOXYADENOSINE("DA", "2", PolymerType.NUCLEOTIDE),
    DEOXYCYTIDINE("DC", "3", PolymerType.NUCLEOTIDE),
    DEOXYGUANOSINE("DG", "4", PolymerType.NUCLEOTIDE), // 25
    GUANOSINE("G", "5", PolymerType.NUCLEOTIDE),
    THYMIDINE("T", "6", PolymerType.NUCLEOTIDE),
    URIDINE("U", "7", PolymerType.NUCLEOTIDE),

    // joker categories
//    LIGAND("LIG", "l"),

    // stuff to ignore for hashCode creation
    UNKNOWN_AMINO_ACID("UNK", "X", null),
    UNKNOWN_NUCLEOTIDE("N", "Z", null),
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
     * The (up to) three-letter code of this component. Matches labelCompId.
     * @return a String
     */
    public String getThreeLetterCode() {
        return threeLetterCode;
    }

    /**
     * The one-letter code of this component. May be 'artificial'/nonstandard. Used e.g. to name inverted index files.
     * @return a String
     */
    public String getOneLetterCode() {
        return oneLetterCode;
    }

    /**
     * The polymer type of this component.
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
