package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.KruskalMotifPruner;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.core.NoOperationMotifPruner;
import org.rcsb.strucmotif.domain.SpriteSearchContext;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.motif.EnrichedMotifDefinition;
import org.rcsb.strucmotif.domain.structure.ResidueGraph;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.SingleStructureInvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class SpriteContextBuilder implements ContextBuilder<SpriteContextBuilder.Mandatory2SpriteBuilder, SpriteSearchContext> {
    private final StructureDataProvider structureDataProvider;
    private final KruskalMotifPruner kruskalMotifPruner;
    private final NoOperationMotifPruner noOperationMotifPruner;
    private final MotifSearchRuntime motifSearchRuntime;
    private final MotifSearchConfig motifSearchConfig;

    /**
     * Construct a new query builder.
     * @param structureDataProvider injectable structure provider
     * @param kruskalMotifPruner injectable motif pruner
     * @param noOperationMotifPruner injectable nop motif pruner
     * @param motifSearchRuntime injectable runtime
     * @param motifSearchConfig injectable config
     */
    @Autowired
    public SpriteContextBuilder(StructureDataProvider structureDataProvider, KruskalMotifPruner kruskalMotifPruner, NoOperationMotifPruner noOperationMotifPruner, MotifSearchRuntime motifSearchRuntime, MotifSearchConfig motifSearchConfig) {
        this.structureDataProvider = structureDataProvider;
        this.kruskalMotifPruner = kruskalMotifPruner;
        this.noOperationMotifPruner = noOperationMotifPruner;
        this.motifSearchRuntime = motifSearchRuntime;
        this.motifSearchConfig = motifSearchConfig;
    }

    /**
     * Define a motif based on the id of the reference structure and a selection of components.
     * @param structureIdentifier the id to acquire
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatorySpriteBuilder defineByPdbId(String structureIdentifier) {
        Structure structure = structureDataProvider.readOriginal(structureIdentifier);
        return defineByStructure(structure);
    }

    /**
     * Routine if stream of structure data contains extracted motif.
     * @param inputStream the data to ready - all components are considered the motif
     * @return mandatory parameter step
     */
    public MandatorySpriteBuilder defineByFile(InputStream inputStream) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream);
        return defineByStructure(structure);
    }

    /**
     * Routine if structure files contains extracted motif. Checks that the structure contains a reasonable number of
     * components to rule out erroneous arguments.
     * @param structure the file to ready - all components are considered the motif
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatorySpriteBuilder defineByStructure(Structure structure) {
        String structureIdentifier = structure.getStructureIdentifier().toUpperCase();

        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig, false);
        InvertedIndex invertedIndex = new SingleStructureInvertedIndex(residueGraph);

        return new MandatorySpriteBuilder(structureIdentifier, structure, invertedIndex);
    }

    public class MandatorySpriteBuilder {
        private final String structureIdentifier;
        private final Structure structure;
        private final InvertedIndex invertedIndex;

        MandatorySpriteBuilder(String structureIdentifier, Structure structure, InvertedIndex invertedIndex) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.invertedIndex = invertedIndex;
        }

        public Mandatory2SpriteBuilder andMotifs(List<EnrichedMotifDefinition> motifDefinitions) {
            return new Mandatory2SpriteBuilder(structureIdentifier, structure, motifDefinitions, invertedIndex);
        }
    }

    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     */
    public class Mandatory2SpriteBuilder implements MandatoryBuilder<Mandatory2SpriteBuilder, SpriteSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<EnrichedMotifDefinition> motifDefinitions;
        private final InvertedIndex invertedIndex;
        private int backboneDistanceTolerance;
        private int sideChainDistanceTolerance;
        private int angleTolerance;
        private float rmsdCutoff;
        private AtomPairingScheme atomPairingScheme;
        private MotifPruner motifPruner;

        Mandatory2SpriteBuilder(String structureIdentifier, Structure structure, List<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.backboneDistanceTolerance = AssamParameters.DEFAULT_BACKBONE_DISTANCE_TOLERANCE;
            this.sideChainDistanceTolerance = AssamParameters.DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE;
            this.angleTolerance = AssamParameters.DEFAULT_ANGLE_TOLERANCE;
            this.rmsdCutoff = Float.MAX_VALUE;
            this.atomPairingScheme = AtomPairingScheme.SIDE_CHAIN;
            // defines the 'default' motif pruning strategy
            this.motifPruner = SpriteContextBuilder.this.kruskalMotifPruner;
        }

        /**
         * Specify the backbone distance tolerance (default: 1).
         * @param backboneDistanceTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        /**
         * Filter hits based on RMSD. Only relevant when scoring strategy involves alignment.
         * @param rmsdCutoff the RMSD cutoff above which hits are filtered
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder rmsdCutoff(double rmsdCutoff) {
            this.rmsdCutoff = (float) rmsdCutoff;
            return this;
        }

        /**
         * Controls which atoms will be considered for alignment. Only relevant when scoring scheme is alignment-based.
         * @param atomPairingScheme how to pair atoms for alignment routine
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        @Override
        public Mandatory2SpriteBuilder motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
            switch (motifPruningStrategy) {
                case KRUSKAL:
                    this.motifPruner = SpriteContextBuilder.this.kruskalMotifPruner;
                    break;
                case NONE:
                    this.motifPruner = SpriteContextBuilder.this.noOperationMotifPruner;
                    break;
                default:
                    throw new UnsupportedOperationException("Unhandled case: " + motifPruningStrategy);
            }
            return this;
        }

        /**
         * Creates a {@link AssamParameters} instance based on all values. Proceeds to the next step.
         * @return the optional argument step
         */
        @Override
        public OptionalSpriteBuilder buildParameters() {
            SpriteParameters parameters = new SpriteParameters(backboneDistanceTolerance,
                    sideChainDistanceTolerance,
                    angleTolerance,
                    rmsdCutoff,
                    atomPairingScheme,
                    motifPruner);
            return new OptionalSpriteBuilder(structureIdentifier, structure, motifDefinitions, invertedIndex, parameters);
        }
    }

    /**
     * Optional parameters of the algorithm.
     */
    public class OptionalSpriteBuilder implements OptionalBuilder<SpriteSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<EnrichedMotifDefinition> motifDefinitions;
        private final InvertedIndex invertedIndex;
        private final SpriteParameters parameters;

        OptionalSpriteBuilder(String structureIdentifier, Structure structure, List<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex, SpriteParameters parameters) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.parameters = parameters;
        }

        /**
         * Build the actual container.
         * @return the immutable instance of all query parameters
         */
        @Override
        public SpriteSearchContext buildContext() {
            SpriteSearchQuery query = new SpriteSearchQuery(structureIdentifier,
                    structure,
                    motifDefinitions,
                    parameters);
            return new SpriteSearchContext(motifSearchRuntime, motifSearchConfig, invertedIndex, query);
        }
    }
}
