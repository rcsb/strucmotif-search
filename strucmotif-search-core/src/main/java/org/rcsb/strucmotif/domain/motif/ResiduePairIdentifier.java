package org.rcsb.strucmotif.domain.motif;

import org.rcsb.strucmotif.domain.structure.LabelSelection;

import java.util.stream.Stream;

/**
 * Residue pair identifiers combine the {@link LabelSelection} of each residue making up this pair.
 */
public interface ResiduePairIdentifier {
    String getLabelAsymId1();

    String getStructOperId1();

    int getLabelSeqId1();

    String getLabelAsymId2();

    String getStructOperId2();

    int getLabelSeqId2();

    LabelSelection getLabelSelection1();

    LabelSelection getLabelSelection2();

    default Stream<LabelSelection> labelSelections() {
        return Stream.of(getLabelSelection1(), getLabelSelection2());
    }
}
