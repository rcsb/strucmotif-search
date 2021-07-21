package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Selection;

import java.util.stream.Stream;

/**
 * Residue pair identifiers combine the {@link Selection} of each residue making up this pair.
 */
public interface ResiduePairIdentifier {
    int getIndex1();

    String getStructOperId1();

    int getIndex2();

    String getStructOperId2();

    IndexSelection getIndexSelection1();

    IndexSelection getIndexSelection2();

    default Stream<IndexSelection> indexSelections() {
        return Stream.of(getIndexSelection1(), getIndexSelection2());
    }
}
