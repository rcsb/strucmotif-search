package org.rcsb.strucmotif.align;

import org.rcsb.strucmotif.domain.AlignmentResult;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Residue;

import java.util.List;

/**
 * Superimpose 2 sets of residues.
 */
public interface AlignmentService {
    /**
     * Align 2 sets of residues to one another.
     * @param referenceStructureIdentifier the reference structure identifier
     * @param referenceSelection the reference set of residues
     * @param candidateStructureIdentifier the candidate structure identifier
     * @param candidateSelection the candidate set of residues to evaluate
     * @param alignmentScheme the atom names to consider for each residue during alignment
     * @return an Alignment instance which provides the aligned instances, transformation operations and scores
     */
    AlignmentResult align(StructureIdentifier referenceStructureIdentifier,
                          List<LabelSelection> referenceSelection,
                          StructureIdentifier candidateStructureIdentifier,
                          List<LabelSelection> candidateSelection,
                          AtomPairingScheme alignmentScheme);

    /**
     * Aligns 2 sets of residues to one another.
     * @param reference the reference set of residues
     * @param candidate the candidate set of residues to evaluate
     * @param atomPairingScheme the atom names to consider for each residue during alignment
     * @return an Alignment instance which provides the aligned instances, transformation operations and scores
     */
    AlignmentResult align(List<Residue> reference, List<Residue> candidate, AtomPairingScheme atomPairingScheme);
}
