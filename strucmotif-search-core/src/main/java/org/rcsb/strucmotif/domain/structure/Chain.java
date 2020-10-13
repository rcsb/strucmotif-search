package org.rcsb.strucmotif.domain.structure;

import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A chain of a structure.
 */
public class Chain {
    private final ChainIdentifier chainIdentifier;
    private final List<Residue> residues;
    private final List<Residue> transformedResidues;
    private final double[][] transformation;
    private final boolean neutral;

    /**
     * Construct a new chain. Lazily, applies a given transformation on the chain. This makes creation lightweight but
     * will require time when the residues are accessed for the first time.
     * @param chainIdentifier its identifier
     * @param residues all residues present in this chain
     * @param transformation the transformation: <code>null</code> to do 'nothing'
     */
    Chain(ChainIdentifier chainIdentifier, List<Residue> residues, double[][] transformation) {
        this.chainIdentifier = chainIdentifier;
        this.residues = residues;

        this.neutral = Arrays.deepEquals(Transformation.IDENTITY_MATRIX_4D, transformation);
        this.transformedResidues = neutral ? residues : new ArrayList<>(residues.size());
        this.transformation = transformation;

    }

    /**
     * Reports the identifier of this chain.
     * @return a {@link ChainIdentifier} instance
     */
    public ChainIdentifier getChainIdentifier() {
        return chainIdentifier;
    }

    /**
     * Be lazy: this allows to create chains without instantly transforming all atoms - will be transformed and
     * instantiated upon first every access - not thread-safe
     * @return residues in this chain
     */
    public List<Residue> getResidues() {
        // not yet transformed and non-neutral operation
        if (transformedResidues.size() != residues.size()) {
            for (Residue residue : residues) {
                transformedResidues.add(StructureFactory.createResidue(residue.getResidueIdentifier(),
                        residue.getAtoms(),
                        transformation));
            }
        }

        return transformedResidues;
    }

    /**
     * Report if this chain was created by transforming original coordinates.
     * @return <code>true</code> if no 'original' chain
     */
    public boolean isTransformed() {
        return !neutral;
    }

    @Override
    public String toString() {
        return chainIdentifier.toString();
    }
}
