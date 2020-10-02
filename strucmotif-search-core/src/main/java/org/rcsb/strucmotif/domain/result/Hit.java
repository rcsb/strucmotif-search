package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.List;

/**
 * Describes the context of a hit that resembles the query motif.
 */
public interface Hit {
    StructureIdentifier getStructureIdentifier();

    List<LabelSelection> getSelection();

    GeometricDescriptorScore getGeometricDescriptorScore();
}
