package org.rcsb.strucmotif.domain;

import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.math.Algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * A wrapper which describes the correspondence between two collections of residues. Internally takes care of
 * converting (in some sense...) residues which atoms which coordinates into mere double[] containing 3D vectors to
 * align.
 */
public class AtomCorrespondence {
    private final List<Residue> originalReference;
    private final List<Residue> originalCandidate;
    private final double[] referenceCentroid;
    private final double[] candidateCentroid;
    private final List<double[]> centeredReference;
    private final List<double[]> centeredCandidate;

    public AtomCorrespondence(List<Residue> originalReference, List<Residue> originalCandidate, AtomPairingScheme atomPairingScheme) {
        this.originalReference = originalReference;
        this.originalCandidate = originalCandidate;

        Map<Atom, Atom> mapping = pairAtomsByName(originalReference, originalCandidate, atomPairingScheme);

        // determine centroid on selected atoms
        this.referenceCentroid = centroid(mapping.keySet());
        this.candidateCentroid = centroid(mapping.values());

        this.centeredReference = center(mapping.keySet(), referenceCentroid);
        this.centeredCandidate = center(mapping.values(), candidateCentroid);
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
    private static Map<Atom, Atom> pairAtomsByName(List<Residue> reference, List<Residue> candidate, AtomPairingScheme atomPairingScheme) {
        Map<Atom, Atom> mapping = new LinkedHashMap<>();
        boolean schemeRequiresSubset = atomPairingScheme != AtomPairingScheme.ALL;

        for (int i = 0; i < reference.size(); i++) {
            Residue referenceGroup = reference.get(i);
            Residue candidateGroup = candidate.get(i);
            ResidueType referenceResidueType = referenceGroup.getResidueIdentifier().getResidueType();
            // extract all atoms relevant for an alignment
            List<Atom> referenceAtoms = referenceGroup.getAtoms()
                    .stream()
                    .filter(referenceAtom -> mappableAtom(referenceResidueType, referenceAtom))
                    .collect(Collectors.toList());

            for (Atom referenceAtom : referenceAtoms) {
                String referenceLabel = referenceAtom.getAtomIdentifier().getLabelAtomId();

                // if using a subset and if this is not an allowed name: continue
                if (schemeRequiresSubset && !atomPairingScheme.test(referenceLabel)) {
                    continue;
                }

                Optional<Atom> candidateAtom = candidateGroup.findAtom(referenceLabel);
                candidateAtom.ifPresent(a -> mapping.put(referenceAtom, a));
            }
        }

        if (mapping.isEmpty()) {
            throw new IllegalStateException("Found empty pairing of atoms");
        }

        return mapping;
    }

    /**
     * Determine if atoms of a certain name are useful to find the best alignment. Ambiguous atom names cause serious
     * problems. Strategy: Ignore them!
     * @param referenceResidueType reference residue type
     * @param referenceAtom the reference atom
     * @return true if both are compatible and should be assessed for RMSD calculation
     */
    private static boolean mappableAtom(ResidueType referenceResidueType, Atom referenceAtom) {
        String referenceLabel = referenceAtom.getAtomIdentifier().getLabelAtomId();
        // see Coutsias, 2019
        switch (referenceResidueType) {
            case ARGININE:
                if (referenceLabel.equals("NH1") || referenceLabel.equals("NH2")) {
                    return false;
                }
            case ASPARTIC_ACID:
                if (referenceLabel.equals("OD1") || referenceLabel.equals("OD2")) {
                    return false;
                }
            case GLUTAMIC_ACID:
                if (referenceLabel.equals("OE1") || referenceLabel.equals("OE2")) {
                    return false;
                }
            case LEUCINE:
                if (referenceLabel.equals("OD1") || referenceLabel.equals("OD2")) {
                    return false;
                }
            case PHENYLALANINE: case TYROSINE:
                if (referenceLabel.equals("CD1") || referenceLabel.equals("CD2") || referenceLabel.equals("CE1") ||
                        referenceLabel.equals("CE2")) {
                    return false;
                }
            case  VALINE:
                if (referenceLabel.equals("OG1") || referenceLabel.equals("OG2")) {
                    return false;
                }
            default:
                return true;
        }
    }

    /**
     * The original reference.
     * @return a collection of components
     */
    public List<Residue> getOriginalReference() {
        return originalReference;
    }

    /**
     * The centered vectors of the reference.
     * @return a collection of double[]
     */
    public List<double[]> getCenteredReference() {
        return centeredReference;
    }

    /**
     * The centroid of the original reference coordinates.
     * @return a 3D vector
     */
    public double[] getReferenceCentroid() {
        return referenceCentroid;
    }

    /**
     * The original candidate.
     * @return a collection of components
     */
    public List<Residue> getOriginalCandidate() {
        return originalCandidate;
    }

    /**
     * The centered vectors of the candidate.
     * @return a collection of double[]
     */
    public List<double[]> getCenteredCandidate() {
        return centeredCandidate;
    }

    /**
     * The centroid of the original candidate coordinates.
     * @return a 3D vector
     */
    public double[] getCandidateCentroid() {
        return candidateCentroid;
    }

    private static double[] centroid(Collection<Atom> collection) {
        // inline centroid code to omit converting to List<double[]>
        double x = 0;
        double y = 0;
        double z = 0;
        for (Atom atom : collection) {
            double[] vec3d = atom.getCoord();
            x += vec3d[0];
            y += vec3d[1];
            z += vec3d[2];
        }
        return new double[] {
                x / collection.size(),
                y / collection.size(),
                z / collection.size()
        };
    }

    private static List<double[]> center(Collection<Atom> container, double[] centroid) {
        List<double[]> values = new ArrayList<>(container.size());
        for (Atom atom : container) {
            values.add(Algebra.subtract3d(atom.getCoord(), centroid));
        }
        return values;
    }
}
