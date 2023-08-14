package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Identifier-based representation of motifs.
 */
public class MotifDefinition {
    /**
     * The HDS motif.
     */
    public static final MotifDefinition HDS = new MotifDefinition("HDS",
            "4cha",
            "catalytic triad",
            "Many proteases use serine as a nucleophilic residue during catalysis. The catalytic triad is composed of three residues, which are commonly distributed over two polypeptide chains. Most proteases are zymogens that require activation by proteolytic processing to prevent uncontrolled digestion of proteins within the cell.",
            List.of(new LabelSelection("B", "1", 42), // H
                    new LabelSelection("B", "1", 87), // D
                    new LabelSelection("C", "1", 47))); // S

    /**
     * The KDDDE motif.
     */
    public static final MotifDefinition KDDDE = new MotifDefinition("KDDDE",
            "1lap",
            "aminopeptidase",
            "Aminopeptidases play important roles in protein degradation by removing residue from the N-terminus of a of polypeptide chains. The active site of leucine aminopeptidase contains two nearby zinc ions.",
            List.of(new LabelSelection("A", "1", 250), // H
                    new LabelSelection("A", "1", 255), // D
                    new LabelSelection("A", "1", 273), // D
                    new LabelSelection("A", "1", 332), // D
                    new LabelSelection("A", "1", 334))); // E

    /**
     * The CHH motif.
     */
    public static final MotifDefinition CHH = new MotifDefinition("CHH",
            "1g2f",
            "simplified zinc finger",
            "DNA binding proteins often contain His2/Cys2 zinc fingers. These motifs are composed of two cysteine and two histidine residues which adopt a ββα structure wrapping around a single zinc ion.",
            List.of(new LabelSelection("F", "1", 7), // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))); // H

    /**
     * The CHCH motif.
     */
    public static final MotifDefinition CHCH = new MotifDefinition("CHCH",
            "1g2f",
            "original zinc finger",
            "DNA binding proteins often contain His2/Cys2 zinc fingers. These motifs are composed of two cysteine and two histidine residues which adopt a ββα structure wrapping around a single zinc ion.",
            List.of(new LabelSelection("F", "1", 7), // C
                    new LabelSelection("F", "1", 12), // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))); // H

    /**
     * The KDEEH motif.
     */
    public static final MotifDefinition KDEEH = new MotifDefinition("KDEEH",
            "2mnr",
            "enolase superfamily",
            "The enolase superfamily refers to a group of proteins diverse in sequence and structure that are all capable of abstracting a proton from a carboxylic acid. Isofunctional exchanges are crucial to represent this superfamily accurately by a structural motif.",
            List.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295))); // H

    /**
     * The KDEEH_EXCHANGES motif.
     */
    public static final MotifDefinition KDEEH_EXCHANGES = new MotifDefinition("KDEEH_EXCHANGES",
            "2mnr",
            "enolase superfamily with exchanges",
            "The enolase superfamily refers to a group of proteins diverse in sequence and structure that are all capable of abstracting a proton from a carboxylic acid. Isofunctional exchanges are crucial to represent this superfamily accurately by a structural motif.",
            List.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295)), // H
            new LinkedHashSet<>(List.of(new PositionSpecificExchange(new LabelSelection("A", "1", 162), ResidueType.LYSINE, ResidueType.HISTIDINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 245), ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 295), ResidueType.HISTIDINE, ResidueType.LYSINE))));

    /**
     * The GGGG motif.
     */
    public static final MotifDefinition GGGG = new MotifDefinition("GGGG",
            "3ibk",
            "RNA G-tetrad",
            "G-tetrads are a common association motif in RNA and DNA. They are exclusively composed of guanine and stabilized by Hoogsteen base pairings. The negative charges of four oxygen atoms coordinate monovalent ions such as K+ and individual tetrads tend to be stacked one on another.",
            List.of(new LabelSelection("A", "1", 4), // G
                    new LabelSelection("A", "1", 10), // G
                    new LabelSelection("B", "1", 4), // G
                    new LabelSelection("B", "1", 10))); // G

    private final String motifIdentifier;
    private final String structureIdentifier;
    private final String title;
    private final String description;
    private final List<LabelSelection> labelSelections;
    private final Set<PositionSpecificExchange> positionSpecificExchanges;

    /**
     * Construct a motif.
     * @param motifIdentifier a unique identifier
     * @param structureIdentifier referenced structure
     * @param title title
     * @param description longer description
     * @param labelSelections referenced residues
     */
    public MotifDefinition(String motifIdentifier, String structureIdentifier, String title, String description, List<LabelSelection> labelSelections) {
        this(motifIdentifier, structureIdentifier, title, description, labelSelections, Collections.emptySet());
    }

    /**
     * Construct a motif.
     * @param motifIdentifier a unique identifier
     * @param structureIdentifier referenced structure
     * @param title title
     * @param description longer description
     * @param labelSelections referenced residues
     * @param positionSpecificExchanges additional exchanges
     */
    public MotifDefinition(String motifIdentifier, String structureIdentifier, String title, String description, List<LabelSelection> labelSelections, Set<PositionSpecificExchange> positionSpecificExchanges) {
        this.motifIdentifier = motifIdentifier;
        this.structureIdentifier = structureIdentifier;
        this.title = title;
        this.description = description;
        this.labelSelections = labelSelections;
        this.positionSpecificExchanges = positionSpecificExchanges;
    }

    /**
     * The structure identifier of this motif.
     * @return a String
     */
    public String getStructureIdentifier() {
        return structureIdentifier;
    }

    /**
     * Short title of this motif.
     * @return a String
     */
    public String getTitle() {
        return title;
    }

    /**
     * Description of this motif.
     * @return a String
     */
    public String getDescription() {
        return description;
    }

    /**
     * The residues referenced by this motif.
     * @return a collection of LabelSelections
     */
    public List<LabelSelection> getLabelSelections() {
        return labelSelections;
    }

    /**
     * Potential position-specific exchanges for this motif.
     * @return a collection of exchanges
     */
    public Set<PositionSpecificExchange> getPositionSpecificExchanges() {
        return positionSpecificExchanges;
    }

    /**
     * A unique identifier.
     * @return a String
     */
    public String getMotifIdentifier() {
        return motifIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MotifDefinition that = (MotifDefinition) o;
        return Objects.equals(motifIdentifier, that.motifIdentifier) && Objects.equals(structureIdentifier, that.structureIdentifier) && Objects.equals(title, that.title) && Objects.equals(description, that.description) && Objects.equals(labelSelections, that.labelSelections) && Objects.equals(positionSpecificExchanges, that.positionSpecificExchanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(motifIdentifier, structureIdentifier, title, description, labelSelections, positionSpecificExchanges);
    }

    @Override
    public String toString() {
        return "MotifDefinition{" +
                "motifIdentifier='" + motifIdentifier + '\'' +
                ", structureIdentifier='" + structureIdentifier + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", labelSelections=" + labelSelections +
                ", positionSpecificExchanges=" + positionSpecificExchanges +
                '}';
    }
}
