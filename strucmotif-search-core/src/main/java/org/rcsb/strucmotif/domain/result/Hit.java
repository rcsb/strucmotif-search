package org.rcsb.strucmotif.domain.result;

import org.rcsb.strucmotif.domain.identifier.AssemblyIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.score.GeometricDescriptorScore;
import org.rcsb.strucmotif.domain.selection.LabelSelection;

import java.util.List;

/**
 * Describes the context of a hit that resembles the query motif.
 */
public interface Hit {
    /**
     * The structure in which this hit occurs.
     * @return a structure identifier
     */
    StructureIdentifier getStructureIdentifier();

    /**
     * The assembly identifier in which this hit occurs.
     * @return an assembly identifier
     */
    AssemblyIdentifier getAssemblyIdentifier();

    /**
     * The selection that produced this hit.
     * @return an ordered set of label selections
     */
    List<LabelSelection> getSelection();

    /**
     * The score that this hit produced.
     * @return a geometric descriptor score
     */
    GeometricDescriptorScore getGeometricDescriptorScore();
}
