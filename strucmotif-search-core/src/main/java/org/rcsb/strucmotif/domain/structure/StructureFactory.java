package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;

import java.util.List;

/**
 * Standardizes construction of model instances.
 */
public class StructureFactory {
    private StructureFactory() {

    }

    /**
     * Construct a new structure.
     * @param structureIdentifier the PDB-id
     * @param title the title (description) of this structure
     * @param chains all chains present
     * @return the constructed Structure instance
     */
    public static Structure createStructure(StructureIdentifier structureIdentifier,
                                            String title,
                                            List<Chain> chains) {
        return new Structure(structureIdentifier, title, chains);
    }

    /**
     * Construct a new chain. Lazily, applies a given transformation on the chain. This makes creation lightweight but
     * will require time when the components are accessed for the first time.
     * @param chainIdentifier its identifier
     * @param residues all components present in this chain
     * @param transformation the transformation: neutral to do 'nothing'
     * @return the constructed Chain instance
     */
    public static Chain createChain(ChainIdentifier chainIdentifier,
                                    List<Residue> residues,
                                    double[][] transformation) {
        return new Chain(chainIdentifier, residues, transformation);
    }

    /**
     * Construct residues.
     * @param residueIdentifier the original identifier
     * @param atoms the atoms to associate
     * @param transformation the transformation to apply (may be neutral)
     * @return the constructed residue
     */
    public static Residue createResidue(ResidueIdentifier residueIdentifier,
                                        List<Atom> atoms,
                                        double[][] transformation) {
        // cannot infer assembly id by residue itself - this method is used to create new assemblies with diverging assembly ids
        ResidueIdentifier updatedResidueIdentifier = new ResidueIdentifier(residueIdentifier.getResidueType(),
                residueIdentifier.getLabelSeqId(),
                residueIdentifier.getIndex());

        // fallback to generic impl if cannot determine type
        if (residueIdentifier.getResidueType() == null || residueIdentifier.getResidueType().getPolymerType() == null) {
            return new Residue(updatedResidueIdentifier, atoms, transformation);
        }

        // otherwise use concrete impl
        switch (residueIdentifier.getResidueType().getPolymerType()) {
            case AMINO_ACID:
                return new AminoAcid(updatedResidueIdentifier, atoms, transformation);
            case NUCLEOTIDE:
                return new Nucleotide(updatedResidueIdentifier, atoms, transformation);
            default:
                throw new UnsupportedOperationException("cannot handle case " + residueIdentifier.getResidueType().getPolymerType());
        }
    }

    /**
     * Construct a new atom.
     * @param atomIdentifier its identifier
     * @param coord its position
     * @return the constructed Atom instance
     */
    public static Atom createAtom(AtomIdentifier atomIdentifier, double[] coord) {
        return new Atom(atomIdentifier, coord);
    }
}
