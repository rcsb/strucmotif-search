package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.query.PositionSpecificExchange;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;

import java.util.Collection;
import java.util.Set;

/**
 * Motif definitions used in examples and tests.
 */
public enum Motifs {
    HDS("4cha", Set.of(new LabelSelection("B", "1", 42), // H
                    new LabelSelection("B", "1", 87), // D
                    new LabelSelection("C", "1", 47))),  // S
    KDDDE("1lap", Set.of(new LabelSelection("A", "1", 250), // H
            new LabelSelection("A", "1", 255), // D
            new LabelSelection("A", "1", 273), // D
            new LabelSelection("A", "1", 332), // D
            new LabelSelection("A", "1", 334))),  // E
    CHH("1g2f", Set.of(new LabelSelection("F", "1", 7),  // C
                    new LabelSelection("F", "1", 25), // H
                    new LabelSelection("F", "1", 29))),  // H
    CHCH("1g2f", Set.of(new LabelSelection("F", "1", 7),  // C
            new LabelSelection("F", "1", 12), // C
            new LabelSelection("F", "1", 25), // H
            new LabelSelection("F", "1", 29))),  // H
    KDEEH("2mnr", Set.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295))),  // H
    KDEEH_EXCHANGES("2mnr", Set.of(new LabelSelection("A", "1", 162), // K
                    new LabelSelection("A", "1", 193), // D
                    new LabelSelection("A", "1", 219), // E
                    new LabelSelection("A", "1", 245), // E
                    new LabelSelection("A", "1", 295)), // H
                    new PositionSpecificExchange(new LabelSelection("A", "1", 162), ResidueType.LYSINE, ResidueType.HISTIDINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 245), ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE),
                    new PositionSpecificExchange(new LabelSelection("A", "1", 295), ResidueType.HISTIDINE, ResidueType.LYSINE)),
    GGGG("3ibk", Set.of(new LabelSelection("A", "1", 4), // G
                    new LabelSelection("A", "1", 10), // G
                    new LabelSelection("B", "1", 4), // G
                    new LabelSelection("B", "1", 10))); // G

    private final StructureIdentifier structureIdentifier;
    private final Collection<LabelSelection> selection;
    private final PositionSpecificExchange[] positionSpecificExchanges;

    Motifs(String pdbId, Collection<LabelSelection> selection, PositionSpecificExchange... positionSpecificExchanges) {
        this.structureIdentifier = new StructureIdentifier(pdbId);
        this.selection = selection;
        this.positionSpecificExchanges = positionSpecificExchanges;
    }

    public StructureIdentifier getStructureIdentifier() {
        return structureIdentifier;
    }

    public Collection<LabelSelection> getSelection() {
        return selection;
    }

    public PositionSpecificExchange[] getPositionSpecificExchanges() {
        return positionSpecificExchanges;
    }
}
