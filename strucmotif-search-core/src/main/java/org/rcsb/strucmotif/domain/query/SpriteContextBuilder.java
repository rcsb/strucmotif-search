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
import org.rcsb.strucmotif.io.SingleStructureDataProvider;
import org.rcsb.strucmotif.io.SingleStructureIndexProvider;
import org.rcsb.strucmotif.io.SingleStructureInvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class SpriteContextBuilder implements ContextBuilder<SpriteContextBuilder.MandatorySpriteBuilder, SpriteSearchContext> {
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
    public SpriteMotifRegistryBuilder defineByPdbId(String structureIdentifier) {
        Structure structure = structureDataProvider.readOriginal(structureIdentifier);
        return defineByStructure(structure);
    }

    /**
     * Routine if stream of structure data contains extracted motif.
     * @param inputStream the data to ready - all components are considered the motif
     * @return mandatory parameter step
     */
    public SpriteMotifRegistryBuilder defineByFile(InputStream inputStream) {
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
    public SpriteMotifRegistryBuilder defineByStructure(Structure structure) {
        String structureIdentifier = structure.getStructureIdentifier().toUpperCase();

        ResidueGraph residueGraph = new ResidueGraph(structure, motifSearchConfig, false);
        InvertedIndex invertedIndex = new SingleStructureInvertedIndex(residueGraph);
        StructureIndexProvider structureIndexProvider = new SingleStructureIndexProvider(structure);
        StructureDataProvider structureDataProvider = new SingleStructureDataProvider(structure);

        return new SpriteMotifRegistryBuilder(structureIdentifier, structure, invertedIndex, structureIndexProvider, structureDataProvider);
    }

    public class SpriteMotifRegistryBuilder {
        private final String structureIdentifier;
        private final Structure structure;
        private final InvertedIndex invertedIndex;
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;

        SpriteMotifRegistryBuilder(String structureIdentifier, Structure structure, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
        }

        public MandatorySpriteBuilder andMotifs(List<EnrichedMotifDefinition> motifDefinitions) {
            return new MandatorySpriteBuilder(structureIdentifier, structure, motifDefinitions, invertedIndex, structureIndexProvider, structureDataProvider);
        }
    }

    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     */
    public class MandatorySpriteBuilder implements MandatoryBuilder<MandatorySpriteBuilder, SpriteSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<EnrichedMotifDefinition> motifDefinitions;
        private final InvertedIndex invertedIndex;
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;
        private int backboneDistanceTolerance;
        private int sideChainDistanceTolerance;
        private int angleTolerance;
        private float rmsdCutoff;
        private AtomPairingScheme atomPairingScheme;
        private MotifPruner motifPruner;

        MandatorySpriteBuilder(String structureIdentifier, Structure structure, List<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
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
        public MandatorySpriteBuilder backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        /**
         * Filter hits based on RMSD. Only relevant when scoring strategy involves alignment.
         * @param rmsdCutoff the RMSD cutoff above which hits are filtered
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder rmsdCutoff(double rmsdCutoff) {
            this.rmsdCutoff = (float) rmsdCutoff;
            return this;
        }

        /**
         * Controls which atoms will be considered for alignment. Only relevant when scoring scheme is alignment-based.
         * @param atomPairingScheme how to pair atoms for alignment routine
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        @Override
        public MandatorySpriteBuilder motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
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
            return new OptionalSpriteBuilder(structureIdentifier, structure, motifDefinitions, invertedIndex, structureIndexProvider, structureDataProvider, parameters);
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
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;
        private final SpriteParameters parameters;

        OptionalSpriteBuilder(String structureIdentifier, Structure structure, List<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, SpriteParameters parameters) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
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
            return new SpriteSearchContext(motifSearchRuntime, motifSearchConfig, invertedIndex, structureIndexProvider, structureDataProvider, query);
        }
    }
}
