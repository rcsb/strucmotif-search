package org.rcsb.strucmotif.domain.structure;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, ResidueType> TLC_MAPPING = Arrays.stream(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getThreeLetterCode, Function.identity()));
    /**
     * Maps labelCompId values to the correct {@link ResidueType}.
     * @param labelCompId three-letter code to process
     * @return the matching type - if argument is null or fail to find: return UNKNOWN_AMINO_ACID
     */
    public static ResidueType ofThreeLetterCode(String labelCompId) {
        ResidueType residueType = TLC_MAPPING.get(labelCompId);
        return residueType != null ? residueType : ResidueType.UNKNOWN_COMPONENT;
    }

    private static final Map<String, ResidueType> OLC_MAPPING = Arrays.stream(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getOneLetterCode, Function.identity()));
    /**
     * Resolve a one-letter-code.
     * @param oneLetterCode the one-letter-code (as defined in this implementation)
     * @return a residue type or null
     */
    public static ResidueType ofOneLetterCode(String oneLetterCode) {
        ResidueType residueType = OLC_MAPPING.get(oneLetterCode);
        return residueType != null ? residueType : ResidueType.UNKNOWN_COMPONENT;
    }
}
