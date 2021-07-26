package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Selection;

import java.util.stream.Stream;

/**
 * Residue pair identifiers combine the {@link Selection} of each residue making up this pair.
 */
public interface ResiduePairIdentifier {
    /**
     * The index of the first residue.
     * @return an int
     */
    int getIndex1();

    /**
     * The operator expression applied to the first residue.
     * @return a String
     */
    String getStructOperId1();

    /**
     * The index of the second residue.
     * @return an int
     */
    int getIndex2();

    /**
     * The operator expression applied to the second residue.
     * @return a String
     */
    String getStructOperId2();

    /**
     * The complex IndexSelection of the first residue. If possible, use low-level methods that avoid object creation if
     * data comes from the inverted index.
     * @return an {@link IndexSelection}
     */
    IndexSelection getIndexSelection1();

    /**
     * The complex IndexSelection of the second residue. If possible, use low-level methods that avoid object creation
     * if data comes from the inverted index.
     * @return an {@link IndexSelection}
     */
    IndexSelection getIndexSelection2();

    /**
     * Convenience method to access both IndexSelections.
     * @return a Stream of IndexSelections
     */
    default Stream<IndexSelection> indexSelections() {
        return Stream.of(getIndexSelection1(), getIndexSelection2());
    }
}
