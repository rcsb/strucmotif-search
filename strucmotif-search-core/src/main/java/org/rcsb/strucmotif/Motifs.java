package org.rcsb.strucmotif;

import org.rcsb.strucmotif.domain.query.MotifSearchQuery;
import org.rcsb.strucmotif.domain.query.QueryBuilder;
import org.rcsb.strucmotif.domain.selection.AuthorSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.AllPurposeReaderImpl;

import java.util.Collection;
import java.util.Set;

public enum Motifs {
    HDS("4cha", Set.of(new AuthorSelection("B", 1, 57), // H
                    new AuthorSelection("B", 1, 102), // D
                    new AuthorSelection("C", 1, 195))),  // S
    KDDDE("1lap", Set.of(new AuthorSelection("A", 1, 250), // H
            new AuthorSelection("A", 1, 255), // D
            new AuthorSelection("A", 1, 273), // D
            new AuthorSelection("A", 1, 332), // D
            new AuthorSelection("A", 1, 334))),  // E
    CHH("1g2f", Set.of(new AuthorSelection("F", 1, 207),  // C
                    new AuthorSelection("F", 1, 225), // H
                    new AuthorSelection("F", 1, 229))),  // H
    CHCH("1g2f", Set.of(new AuthorSelection("F", 1, 207),  // C
            new AuthorSelection("F", 1, 212), // C
            new AuthorSelection("F", 1, 225), // H
            new AuthorSelection("F", 1, 229))),  // H
    KDEEH("2mnr", Set.of(new AuthorSelection("A", 1, 164), // K
                    new AuthorSelection("A", 1, 195), // D
                    new AuthorSelection("A", 1, 221), // E
                    new AuthorSelection("A", 1, 247), // E
                    new AuthorSelection("A", 1, 297))),  // H
    KDEEH_EXCHANGES("2mnr", Set.of(new AuthorSelection("A", 1, 164), // K
                    new AuthorSelection("A", 1, 195), // D
                    new AuthorSelection("A", 1, 221), // E
                    new AuthorSelection("A", 1, 247), // E
                    new AuthorSelection("A", 1, 297)), // H
                    new PositionSpecificExchange(new AuthorSelection("A", 1, 164), ResidueType.LYSINE, ResidueType.HISTIDINE),
                    new PositionSpecificExchange(new AuthorSelection("A", 1, 247), ResidueType.GLUTAMIC_ACID, ResidueType.ASPARTIC_ACID, ResidueType.ASPARAGINE),
                    new PositionSpecificExchange(new AuthorSelection("A", 1, 297), ResidueType.HISTIDINE, ResidueType.LYSINE)),
    GGGG("3ibk", Set.of(new AuthorSelection("A", 1, 4), // G
                    new AuthorSelection("A", 1, 10), // G
                    new AuthorSelection("B", 1, 16), // G
                    new AuthorSelection("B", 1, 22))); // G

    private final Structure structure;
    private final PositionSpecificExchange[] positionSpecificExchanges;

    Motifs(String pdbId, Collection<AuthorSelection> selection, PositionSpecificExchange... positionSpecificExchanges) {
        this.structure = new AllPurposeReaderImpl().readById(pdbId, selection);
        this.positionSpecificExchanges = positionSpecificExchanges;
    }

    public Structure getStructure() {
        return structure;
    }

    public PositionSpecificExchange[] getPositionSpecificExchanges() {
        return positionSpecificExchanges;
    }
}
