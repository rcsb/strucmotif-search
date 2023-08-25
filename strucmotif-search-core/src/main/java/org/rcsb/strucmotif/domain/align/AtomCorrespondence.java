package org.rcsb.strucmotif.domain.align;

import org.rcsb.strucmotif.align.QuaternionAlignmentService;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper which describes the correspondence between two collections of residues. Internally takes care of
 * converting (in some sense...) residues which atoms which coordinates into mere double[] containing 3D vectors to
 * align.
 */
public class AtomCorrespondence {
    private final float[] referenceCentroid;
    private final float[] candidateCentroid;
    private final List<float[]> centeredReferenceVectors;
    private final List<float[]> centeredCandidateVectors;

    /**
     * Finds the correspondence between reference and candidate.
     * @param reference reference container
     * @param candidate candidate container
     * @param atomPairingScheme how to pair atoms?
     */
    public AtomCorrespondence(List<Map<LabelAtomId, float[]>> reference,
                              List<Map<LabelAtomId, float[]>> candidate,
                              AtomPairingScheme atomPairingScheme) {
        Map<float[], float[]> mapping = pairAtomsByName(reference, candidate, atomPairingScheme);

        // determine centroid on selected atoms
        this.referenceCentroid = Algebra.centroid3d(mapping.keySet());
        this.candidateCentroid = Algebra.centroid3d(mapping.values());

        this.centeredReferenceVectors = center(mapping.keySet(), referenceCentroid);
        this.centeredCandidateVectors = center(mapping.values(), candidateCentroid);
    }

    /**
     * Pairs (i.e. finds correspondence) between reference and candidate atoms. Honors a given {@link AtomPairingScheme},
     * i.e. which atoms are considered and which combinations are compatible.
     * @param reference the reference container
     * @param candidate the candidate container
     * @param atomPairingScheme how to pair atoms
     * @throws IllegalStateException when no atoms were paired - this should not happen
     * @return correspondence between atoms of reference and candidate - may ignore some atoms - present as Map
     */
    private static Map<float[], float[]> pairAtomsByName(List<Map<LabelAtomId, float[]>> reference,
                                                         List<Map<LabelAtomId, float[]>> candidate,
                                                         AtomPairingScheme atomPairingScheme) {
        Map<float[], float[]> mapping = new LinkedHashMap<>();
        boolean schemeRequiresSubset = atomPairingScheme != AtomPairingScheme.ALL;

        for (int i = 0; i < reference.size(); i++) {
            boolean added = false;
            Map<LabelAtomId, float[]> referenceGroup = reference.get(i);
            Map<LabelAtomId, float[]> candidateGroup = candidate.get(i);

            for (Map.Entry<LabelAtomId, float[]> referenceAtom : referenceGroup.entrySet()) {
                LabelAtomId referenceLabel = referenceAtom.getKey();
                float[] referenceVector = referenceAtom.getValue();

                // if using a subset and if this is not an allowed name: continue
                if (schemeRequiresSubset && !atomPairingScheme.test(referenceLabel)) {
                    continue;
                }

                float[] candidateAtom = candidateGroup.get(referenceLabel);
                if (candidateAtom != null) {
                    mapping.put(referenceVector, candidateAtom);
                    added = true;
                }
            }

            // handle glycines somewhat gracefully -- ideally, this would only apply to real glycines and not anything without CB
            if (!added && atomPairingScheme == AtomPairingScheme.SIDE_CHAIN) {
                float[] virtualRef = QuaternionAlignmentService.getVirtualCB(referenceGroup);
                float[] virtualCand = QuaternionAlignmentService.getVirtualCB(candidateGroup);
                if (virtualRef != null && virtualCand != null) {
                    mapping.put(virtualRef, virtualCand);
                }
            }
        }

        if (mapping.isEmpty()) {
            throw new IllegalStateException("Found empty pairing of atoms");
        }

        return mapping;
    }

    /**
     * The centered vectors of the reference.
     * @return a collection of double[]
     */
    public List<float[]> getCenteredReferenceVectors() {
        return centeredReferenceVectors;
    }

    /**
     * The centroid of the original reference coordinates.
     * @return a 3D vector
     */
    public float[] getReferenceCentroid() {
        return referenceCentroid;
    }

    /**
     * The centered vectors of the candidate.
     * @return a collection of double[]
     */
    public List<float[]> getCenteredCandidateVectors() {
        return centeredCandidateVectors;
    }

    /**
     * The centroid of the original candidate coordinates.
     * @return a 3D vector
     */
    public float[] getCandidateCentroid() {
        return candidateCentroid;
    }

    private static List<float[]> center(Collection<float[]> container, float[] centroid) {
        List<float[]> out = new ArrayList<>();
        for (float[] atom : container) {
            float[] v = new float[3];
            Algebra.subtract3d(v, atom, centroid);
            out.add(v);
        }
        return out;
    }
}
