package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Identifier-based representation of motifs.
 */
public class MotifDefinition {
    public static final MotifDefinition HDS = new MotifDefinition("4cha",
            "catalytic triad",
            "Many proteases use serine as a nucleophilic residue during catalysis. The catalytic triad is composed of three residues, which are commonly distributed over two polypeptide chains. Most proteases are zymogens that require activation by proteolytic processing to prevent uncontrolled digestion of proteins within the cell.",
            List.of(new LabelSelection("B", "1", 42), // H
                    new LabelSelection("B", "1", 87), // D
                    new LabelSelection("C", "1", 47))); // S

    public static final MotifDefinition KDDDE = new MotifDefinition("1lap",
            "aminopeptidase",
            "Aminopeptidases play important roles in protein degradation by removing residue from the N-terminus of a of polypeptide chains. The active site of leucine aminopeptidase contains two nearby zinc ions.",
            List.of(new LabelSelection("A", "1", 250), // H
                    new LabelSelection("A", "1", 255), // D
                    new LabelSelection("A", "1", 273), // D
                    new LabelSelection("A", "1", 332), // D
                    new LabelSelection("A", "1", 334))); // E

    public static final MotifDefinition CHH = new MotifDefinition("1g2f",
            "simplified zinc finger",
            "DNA binding proteins often contain His2/Cys2 zinc fingers. These motifs are composed of two cysteine and two histidine residues which adopt a ββα structure wrapping around a single zinc ion.",
            List.of(new LabelSelection("F", "1", 7), // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))); // H

    public static final MotifDefinition CHCH = new MotifDefinition("1g2f",
            "original zinc finger",
            "DNA binding proteins often contain His2/Cys2 zinc fingers. These motifs are composed of two cysteine and two histidine residues which adopt a ββα structure wrapping around a single zinc ion.",
            List.of(new LabelSelection("F", "1", 7), // C
                    new LabelSelection("F", "1", 12), // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))); // H

    public static final MotifDefinition KDEEH = new MotifDefinition("2mnr",
            "enolase superfamily",
            "The enolase superfamily refers to a group of proteins diverse in sequence and structure that are all capable of abstracting a proton from a carboxylic acid. Isofunctional exchanges are crucial to represent this superfamily accurately by a structural motif.",
            List.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295))); // H

    public static final MotifDefinition KDEEH_EXCHANGES = new MotifDefinition("2mnr",
            "enolase superfamily with exchanges",
            "The enolase superfamily refers to a group of proteins diverse in sequence and structure that are all capable of abstracting a proton from a carboxylic acid. Isofunctional exchanges are crucial to represent this superfamily accurately by a structural motif.",
            List.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295)), // H
            Set.of(new PositionSpecificExchange(new LabelSelection("A", "1", 162), ResidueType.LYSINE, ResidueType.HISTIDINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 245), ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 295), ResidueType.HISTIDINE, ResidueType.LYSINE)));

    public static final MotifDefinition GGGG = new MotifDefinition("3ibk",
            "RNA G-tetrad",
            "G-tetrads are a common association motif in RNA and DNA. They are exclusively composed of guanine and stabilized by Hoogsteen base pairings. The negative charges of four oxygen atoms coordinate monovalent ions such as K+ and individual tetrads tend to be stacked one on another.",
            List.of(new LabelSelection("A", "1", 4), // G
                    new LabelSelection("A", "1", 10), // G
                    new LabelSelection("B", "1", 4), // G
                    new LabelSelection("B", "1", 10))); // G

    private final String structureIdentifier;
    private final String title;
    private final String description;
    private final List<LabelSelection> labelSelections;
    private final Set<PositionSpecificExchange> positionSpecificExchanges;

    public MotifDefinition(String structureIdentifier, String title, String description, List<LabelSelection> labelSelections) {
        this(structureIdentifier, title, description, labelSelections, Collections.emptySet());
    }

    public MotifDefinition(String structureIdentifier, String title, String description, List<LabelSelection> labelSelections, Set<PositionSpecificExchange> positionSpecificExchanges) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MotifDefinition that = (MotifDefinition) o;
        return structureIdentifier.equals(that.structureIdentifier) && labelSelections.equals(that.labelSelections) && positionSpecificExchanges.equals(that.positionSpecificExchanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(structureIdentifier, labelSelections, positionSpecificExchanges);
    }

    @Override
    public String toString() {
        return "MotifDefinition{" +
                "structureIdentifier='" + structureIdentifier + '\'' +
                ", description='" + description + '\'' +
                ", labelSelections=" + labelSelections +
                ", positionSpecificExchanges=" + positionSpecificExchanges +
                '}';
    }
}
