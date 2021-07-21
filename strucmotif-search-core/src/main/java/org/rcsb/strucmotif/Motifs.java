package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Collection;
import java.util.Set;

/**
 * Motif definitions used in examples and tests.
 */
public enum Motifs {
    /**
     * Catalytic triad.
     */
    HDS("4cha", Set.of(new LabelSelection("B", "1", 42), // H
                    new LabelSelection("B", "1", 87), // D
                    new LabelSelection("C", "1", 47))),  // S
    /**
     * Aminopeptidase.
     */
    KDDDE("1lap", Set.of(new LabelSelection("A", "1", 250), // H
            new LabelSelection("A", "1", 255), // D
            new LabelSelection("A", "1", 273), // D
            new LabelSelection("A", "1", 332), // D
            new LabelSelection("A", "1", 334))),  // E
    /**
     * Simplified zinc finger.
     */
    CHH("1g2f", Set.of(new LabelSelection("F", "1", 7),  // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))),  // H
    /**
     * Original zinc finger.
     */
    CHCH("1g2f", Set.of(new LabelSelection("F", "1", 7),  // C
            new LabelSelection("F", "1", 12), // C
            new LabelSelection("F", "1", 25), // H
            new LabelSelection("F", "1", 29))),  // H
    /**
     * Enolase superfamily.
     */
    KDEEH("2mnr", Set.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295))),  // H
    /**
     * Enolase superfamily with exchanges.
     */
    KDEEH_EXCHANGES("2mnr", Set.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295)), // H
                    new PositionSpecificExchange(new LabelSelection("A", "1", 162), ResidueType.LYSINE, ResidueType.HISTIDINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 245), ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 295), ResidueType.HISTIDINE, ResidueType.LYSINE)),
    /**
     * RNA G-tetrad.
     */
    GGGG("3ibk", Set.of(new LabelSelection("A", "1", 4), // G
                    new LabelSelection("A", "1", 10), // G
                    new LabelSelection("B", "1", 4), // G
                    new LabelSelection("B", "1", 10))); // G

    private final String structureIdentifier;
    private final Collection<LabelSelection> selection;
    private final PositionSpecificExchange[] positionSpecificExchanges;

    Motifs(String pdbId, Collection<LabelSelection> selection, PositionSpecificExchange... positionSpecificExchanges) {
        this.structureIdentifier = pdbId.toLowerCase();
        this.selection = selection;
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
     * The residues referenced by this motif.
     * @return a collection of LabelSelections
     */
    public Collection<LabelSelection> getSelection() {
        return selection;
    }

    /**
     * Potential position-specific exchanges for this motif.
     * @return an array of exchanges
     */
    public PositionSpecificExchange[] getPositionSpecificExchanges() {
        return positionSpecificExchanges;
    }
}
