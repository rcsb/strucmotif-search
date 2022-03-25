package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.KruskalMotifPruner;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.StrucmotifRuntime;
import org.rcsb.strucmotif.core.NoOperationMotifPruner;
import org.rcsb.strucmotif.domain.MotifSearchContext;
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
import java.util.Set;

import static org.rcsb.strucmotif.domain.structure.ResidueGraph.ResidueGraphOptions.assembly;

/**
 * The entry point to create {@link MotifSearchContext} instances.
 */
@Service
public class MotifContextBuilder implements ContextBuilder<MotifContextBuilder.MandatoryBuilderStep, MotifSearchContext> {
    private final StructureDataProvider structureDataProvider;
    private final KruskalMotifPruner kruskalMotifPruner;
    private final NoOperationMotifPruner noOperationMotifPruner;
    private final StrucmotifRuntime strucmotifRuntime;
    private final StrucmotifConfig strucmotifConfig;

    /**
     * Construct a new query builder.
     * @param structureDataProvider injectable structure provider
     * @param kruskalMotifPruner injectable motif pruner
     * @param noOperationMotifPruner injectable nop motif pruner
     * @param strucmotifRuntime injectable runtime
     * @param strucmotifConfig injectable config
     */
    @Autowired
    public MotifContextBuilder(StructureDataProvider structureDataProvider, KruskalMotifPruner kruskalMotifPruner, NoOperationMotifPruner noOperationMotifPruner, StrucmotifRuntime strucmotifRuntime, StrucmotifConfig strucmotifConfig) {
        this.structureDataProvider = structureDataProvider;
        this.kruskalMotifPruner = kruskalMotifPruner;
        this.noOperationMotifPruner = noOperationMotifPruner;
        this.strucmotifRuntime = strucmotifRuntime;
        this.strucmotifConfig = strucmotifConfig;
    }

    /**
     * Define a motif based on the id of the reference structure and a selection of components.
     * @param structureIdentifier the id to acquire
     * @param assemblyIdentifier which assembly to operate on
     * @return mandatory registry step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MotifRegistryBuilder defineByPdbId(String structureIdentifier, String assemblyIdentifier) {
        Structure structure = structureDataProvider.readOriginal(structureIdentifier);
        return defineByStructure(structure, assemblyIdentifier);
    }

    /**
     * Routine if stream of structure data contains extracted motif.
     * @param inputStream the data to ready - all components are considered the motif
     * @param assemblyIdentifier which assembly to operate on
     * @return mandatory registry step
     */
    public MotifRegistryBuilder defineByFile(InputStream inputStream, String assemblyIdentifier) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream);
        return defineByStructure(structure, assemblyIdentifier);
    }

    /**
     * Routine if structure files contains extracted motif. Checks that the structure contains a reasonable number of
     * components to rule out erroneous arguments.
     * @param structure the file to ready - all components are considered the motif
     * @param assemblyIdentifier which assembly to operate on
     * @return mandatory registry step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MotifRegistryBuilder defineByStructure(Structure structure, String assemblyIdentifier) {
        String structureIdentifier = structure.getStructureIdentifier().toUpperCase();

        ResidueGraph residueGraph = new ResidueGraph(structure, strucmotifConfig, assembly(assemblyIdentifier));
        InvertedIndex invertedIndex = new SingleStructureInvertedIndex(residueGraph);
        StructureIndexProvider structureIndexProvider = new SingleStructureIndexProvider(structure);
        StructureDataProvider structureDataProvider = new SingleStructureDataProvider(structure);

        return new MotifRegistryBuilder(structureIdentifier, structure, invertedIndex, structureIndexProvider, structureDataProvider);
    }

    /**
     * 2nd mandatory step that defines the collection of motifs to screen for.
     */
    public class MotifRegistryBuilder {
        private final String structureIdentifier;
        private final Structure structure;
        private final InvertedIndex invertedIndex;
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;

        MotifRegistryBuilder(String structureIdentifier, Structure structure, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
        }

        /**
         * Provide the collection of motifs to screen for. Must be {@link EnrichedMotifDefinition}, which are like
         * normal {@link org.rcsb.strucmotif.domain.motif.MotifDefinition} instances, but they also capture the actual
         * {@link Structure} as well as all relevant residues. Make sure to create enriched motifs once outside and then
         * pass them in, potentially reusing them indefinitely.
         * @param motifDefinitions all motifs to consider
         * @return the next step
         */
        public MandatoryBuilderStep withMotifs(Set<EnrichedMotifDefinition> motifDefinitions) {
            return new MandatoryBuilderStep(structureIdentifier, structure, motifDefinitions, invertedIndex, structureIndexProvider, structureDataProvider);
        }
    }

    /**
     * Builder for everything that must be set (but might fall back to default values).
     */
    public class MandatoryBuilderStep implements MandatoryBuilder<MandatoryBuilderStep, MotifSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final Set<EnrichedMotifDefinition> motifDefinitions;
        private final InvertedIndex invertedIndex;
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;
        private int backboneDistanceTolerance;
        private int sideChainDistanceTolerance;
        private int angleTolerance;
        private float rmsdCutoff;
        private AtomPairingScheme atomPairingScheme;
        private MotifPruner motifPruner;

        MandatoryBuilderStep(String structureIdentifier, Structure structure, Set<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
            this.backboneDistanceTolerance = StructureParameters.DEFAULT_BACKBONE_DISTANCE_TOLERANCE;
            this.sideChainDistanceTolerance = StructureParameters.DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE;
            this.angleTolerance = StructureParameters.DEFAULT_ANGLE_TOLERANCE;
            this.rmsdCutoff = Float.MAX_VALUE;
            this.atomPairingScheme = AtomPairingScheme.SIDE_CHAIN;
            // defines the 'default' motif pruning strategy
            this.motifPruner = MotifContextBuilder.this.kruskalMotifPruner;
        }

        /**
         * Specify the backbone distance tolerance (default: 1).
         * @param backboneDistanceTolerance an int
         * @return this builder
         */
        public MandatoryBuilderStep backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance an int
         * @return this builder
         */
        public MandatoryBuilderStep sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance an int
         * @return this builder
         */
        public MandatoryBuilderStep angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        /**
         * Filter hits based on RMSD. Only relevant when scoring strategy involves alignment.
         * @param rmsdCutoff the RMSD cutoff above which hits are filtered
         * @return this builder
         */
        public MandatoryBuilderStep rmsdCutoff(double rmsdCutoff) {
            this.rmsdCutoff = (float) rmsdCutoff;
            return this;
        }

        /**
         * Controls which atoms will be considered for alignment. Only relevant when scoring scheme is alignment-based.
         * @param atomPairingScheme how to pair atoms for alignment routine
         * @return this builder
         */
        public MandatoryBuilderStep atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        public MandatoryBuilderStep motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        public MandatoryBuilderStep motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
            switch (motifPruningStrategy) {
                case KRUSKAL:
                    this.motifPruner = MotifContextBuilder.this.kruskalMotifPruner;
                    break;
                case NONE:
                    this.motifPruner = MotifContextBuilder.this.noOperationMotifPruner;
                    break;
                default:
                    throw new UnsupportedOperationException("Unhandled case: " + motifPruningStrategy);
            }
            return this;
        }

        @Override
        public OptionalBuilderStep buildParameters() {
            MotifParameters parameters = new MotifParameters(backboneDistanceTolerance,
                    sideChainDistanceTolerance,
                    angleTolerance,
                    rmsdCutoff,
                    atomPairingScheme,
                    motifPruner);
            return new OptionalBuilderStep(structureIdentifier, structure, motifDefinitions, invertedIndex, structureIndexProvider, structureDataProvider, parameters);
        }
    }

    /**
     * Optional parameters of the algorithm.
     */
    public class OptionalBuilderStep implements OptionalBuilder<MotifSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final Set<EnrichedMotifDefinition> motifDefinitions;
        private final InvertedIndex invertedIndex;
        private final StructureIndexProvider structureIndexProvider;
        private final StructureDataProvider structureDataProvider;
        private final MotifParameters parameters;

        OptionalBuilderStep(String structureIdentifier, Structure structure, Set<EnrichedMotifDefinition> motifDefinitions, InvertedIndex invertedIndex, StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, MotifParameters parameters) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.motifDefinitions = motifDefinitions;
            this.invertedIndex = invertedIndex;
            this.structureIndexProvider = structureIndexProvider;
            this.structureDataProvider = structureDataProvider;
            this.parameters = parameters;
        }

        @Override
        public MotifSearchContext buildContext() {
            MotifSearchQuery query = new MotifSearchQuery(structureIdentifier,
                    structure,
                    motifDefinitions,
                    parameters);
            return new MotifSearchContext(strucmotifRuntime, strucmotifConfig, invertedIndex, structureIndexProvider, structureDataProvider, query);
        }
    }
}
