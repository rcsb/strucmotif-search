package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.KruskalMotifPruner;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.core.NoOperationMotifPruner;
import org.rcsb.strucmotif.domain.AssamSearchContext;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The entry point to create {@link AssamSearchQuery} instances.
 */
@Service
public class AssamContextBuilder implements ContextBuilder<AssamContextBuilder.MandatoryAssamBuilder, AssamSearchContext> {
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
    public AssamContextBuilder(StructureDataProvider structureDataProvider, KruskalMotifPruner kruskalMotifPruner, NoOperationMotifPruner noOperationMotifPruner, MotifSearchRuntime motifSearchRuntime, MotifSearchConfig motifSearchConfig) {
        this.structureDataProvider = structureDataProvider;
        this.kruskalMotifPruner = kruskalMotifPruner;
        this.noOperationMotifPruner = noOperationMotifPruner;
        this.motifSearchRuntime = motifSearchRuntime;
        this.motifSearchConfig = motifSearchConfig;
    }

    /**
     * Define a motif based on the id of the reference structure and a selection of components.
     * @param structureIdentifier the id to acquire
     * @param selection which components to select to define the motif
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatoryAssamBuilder defineByPdbIdAndSelection(String structureIdentifier, List<LabelSelection> selection) {
        Structure structure = structureDataProvider.readOriginal(structureIdentifier);
        return defineByStructureAndSelection(structure, selection);
    }

    /**
     * Define a motif based on a stream of structure data and a selection of components.
     * @param inputStream the data to read
     * @param selection which components to select to define the motif
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatoryAssamBuilder defineByFileAndSelection(InputStream inputStream, List<LabelSelection> selection) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream);
        return defineByStructureAndSelection(structure, selection);
    }

    /**
     * Routine if structure files contains extracted motif. Checks that the structure contains a reasonable number of
     * components to rule out erroneous arguments.
     * @param structure the file to ready - all components are considered the motif
     * @param labelSelections the residues of interest
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatoryAssamBuilder defineByStructureAndSelection(Structure structure, List<LabelSelection> labelSelections) {
        try {
            List<Map<LabelAtomId, float[]>> residues = structure.manifestResidues(labelSelections);

            if (residues.size() > motifSearchConfig.getMaxMotifSize()) {
                throw new IllegalArgumentException("maximum motif size is " + motifSearchConfig.getMaxMotifSize() + " - " +
                        "file contains " + residues.size() + " residues");
            }

            String structureIdentifier = structure.getStructureIdentifier().toUpperCase();
            return new MandatoryAssamBuilder(structureIdentifier, structure, labelSelections, residues);
        } catch (NoSuchElementException e) {
            // this happens when trying to access residues that are not part of the structure
            throw new IllegalQueryDefinitionException(e.getMessage());
        }
    }

    /**
     * Routine if stream of structure data contains extracted motif.
     * @param inputStream the data to ready - all components are considered the motif
     * @return mandatory parameter step
     */
    public MandatoryAssamBuilder defineByFile(InputStream inputStream) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream);
        List<LabelSelection> labelSelections = structure.getLabelSelections()
                .stream()
                .map(l -> new LabelSelection(l.getLabelAsymId(), "1", l.getLabelSeqId()))
                .collect(Collectors.toList());
        return defineByStructureAndSelection(structure, labelSelections);
    }

    /**
     * Use a motif definition to start.
     * @param motifDefinition a pre-defined motif
     * @return mandatory parameter step
     */
    public MandatoryAssamBuilder defineByMotif(MotifDefinition motifDefinition) {
        return defineByPdbIdAndSelection(motifDefinition.getStructureIdentifier(), motifDefinition.getLabelSelections())
                .propagateExchanges(motifDefinition.getPositionSpecificExchanges());
    }

    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     */
    public class MandatoryAssamBuilder implements ContextBuilder.MandatoryBuilder<MandatoryAssamBuilder, AssamSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<LabelSelection> labelSelections;
        private final List<Map<LabelAtomId, float[]>> residues;
        private int backboneDistanceTolerance;
        private int sideChainDistanceTolerance;
        private int angleTolerance;
        private float rmsdCutoff;
        private AtomPairingScheme atomPairingScheme;
        private MotifPruner motifPruner;
        private int limit;
        private boolean undefinedAssemblies;
        private Set<PositionSpecificExchange> upstreamExchanges;

        MandatoryAssamBuilder(String structureIdentifier, Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.labelSelections = labelSelections;
            this.residues = residues;
            this.backboneDistanceTolerance = Parameters.DEFAULT_BACKBONE_DISTANCE_TOLERANCE;
            this.sideChainDistanceTolerance = Parameters.DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE;
            this.angleTolerance = Parameters.DEFAULT_ANGLE_TOLERANCE;
            this.rmsdCutoff = Float.MAX_VALUE;
            this.atomPairingScheme = AtomPairingScheme.SIDE_CHAIN;
            // defines the 'default' motif pruning strategy
            this.motifPruner = AssamContextBuilder.this.kruskalMotifPruner;
            this.limit = Integer.MAX_VALUE;
            this.undefinedAssemblies = false;
        }

        /**
         * Specify the backbone distance tolerance (default: 1).
         * @param backboneDistanceTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance the tolerance to use
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        /**
         * Filter hits based on RMSD. Only relevant when scoring strategy involves alignment.
         * @param rmsdCutoff the RMSD cutoff above which hits are filtered
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder rmsdCutoff(double rmsdCutoff) {
            this.rmsdCutoff = (float) rmsdCutoff;
            return this;
        }

        /**
         * Controls which atoms will be considered for alignment. Only relevant when scoring scheme is alignment-based.
         * @param atomPairingScheme how to pair atoms for alignment routine
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        @Override
        public MandatoryAssamBuilder motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
            switch (motifPruningStrategy) {
                case KRUSKAL:
                    this.motifPruner = AssamContextBuilder.this.kruskalMotifPruner;
                    break;
                case NONE:
                    this.motifPruner = AssamContextBuilder.this.noOperationMotifPruner;
                    break;
                default:
                    throw new UnsupportedOperationException("Unhandled case: " + motifPruningStrategy);
            }
            return this;
        }

        /**
         * Stop after a certain number of accepted hits.
         * @param limit the maximum number of hits below the RMSD threshold
         * @return this builder
         */
        public MandatoryAssamBuilder limitResults(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Return undefined assemblies (if indexed).
         * @param undefinedAssemblies a boolean
         * @return this builder
         */
        public MandatoryAssamBuilder undefinedAssemblies(boolean undefinedAssemblies) {
            this.undefinedAssemblies = undefinedAssemblies;
            return this;
        }

        /**
         * Allow to set downstream exchanges via upstream definition.
         * @param upstreamExchanges exchanges from a motif definition
         * @return this builder
         */
        MandatoryAssamBuilder propagateExchanges(Set<PositionSpecificExchange> upstreamExchanges) {
            this.upstreamExchanges = upstreamExchanges;
            return this;
        }

        /**
         * Creates a {@link AssamParameters} instance based on all values. Proceeds to the next step.
         * @return the optional argument step
         */
        public OptionalAssamBuilder buildParameters() {
            AssamParameters parameters = new AssamParameters(backboneDistanceTolerance,
                    sideChainDistanceTolerance,
                    angleTolerance,
                    rmsdCutoff,
                    atomPairingScheme,
                    motifPruner,
                    limit,
                    undefinedAssemblies);
            return new OptionalAssamBuilder(structureIdentifier, structure, labelSelections, residues, parameters, upstreamExchanges);
        }
    }

    /**
     * Optional parameters of the algorithm.
     */
    public class OptionalAssamBuilder implements OptionalBuilder<AssamSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<LabelSelection> labelSelections;
        private final List<Map<LabelAtomId, float[]>> residues;
        private final AssamParameters parameters;
        private final Map<LabelSelection, Set<ResidueType>> exchanges;
        private final Set<String> whitelist;
        private final Set<String> blacklist;
        private StructureDeterminationMethodology structureDeterminationMethodology;

        OptionalAssamBuilder(String structureIdentifier, Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues, AssamParameters parameters, Set<PositionSpecificExchange> upstreamExchanges) {
            this.structureIdentifier = structureIdentifier;
            this.structure = structure;
            this.labelSelections = labelSelections;
            this.residues = residues;
            this.parameters = parameters;
            this.exchanges = upstreamExchanges == null || upstreamExchanges.isEmpty() ? new HashMap<>() : explodeExchanges(upstreamExchanges);
            this.whitelist = new HashSet<>();
            this.blacklist = new HashSet<>();
            this.structureDeterminationMethodology = StructureDeterminationMethodology.ALL;
        }

        private Map<LabelSelection, Set<ResidueType>> explodeExchanges(Set<PositionSpecificExchange> exchanges) {
            return exchanges.stream()
                    .collect(Collectors.toMap(PositionSpecificExchange::getLabelSelection, PositionSpecificExchange::getResidueTypes));
        }

        /**
         * Register an exchange by specifying a 'range'/collection of component types.
         * @param labelSelection the position referenced
         * @param residueTypes all allowed types
         * @return this builder
         */
        public OptionalAssamBuilder addPositionSpecificExchange(LabelSelection labelSelection, Collection<ResidueType> residueTypes) {
            Set<ResidueType> exchange = exchanges.computeIfAbsent(labelSelection, k -> new LinkedHashSet<>());
            exchange.addAll(residueTypes);
            return this;
        }

        /**
         * Narrow down the search space to the specified entries. Leave empty to search everything.
         * @param structureIdentifiers a collection of structure identifiers
         * @return this builder
         */
        public OptionalAssamBuilder whitelist(Collection<String> structureIdentifiers) {
            this.whitelist.addAll(structureIdentifiers);
            return this;
        }

        /**
         * Filter out hits from the specified entries.
         * @param structureIdentifiers a collection of structure identifiers
         * @return this builder
         */
        public OptionalAssamBuilder blacklist(Collection<String> structureIdentifiers) {
            this.blacklist.addAll(structureIdentifiers);
            return this;
        }

        /**
         * Narrow down a search to a specific target set.
         * @param structureDeterminationMethodology the search space of choice
         * @return this builder
         */
        public OptionalAssamBuilder structureDeterminationMethodology(StructureDeterminationMethodology structureDeterminationMethodology) {
            this.structureDeterminationMethodology = structureDeterminationMethodology;
            return this;
        }

        /**
         * Build the actual container.
         * @return the immutable instance of all query parameters
         */
        @Override
        public AssamSearchContext buildContext() {
            AssamSearchQuery query = new AssamSearchQuery(structureIdentifier,
                    structure,
                    labelSelections,
                    residues,
                    parameters,
                    exchanges,
                    whitelist,
                    blacklist,
                    structureDeterminationMethodology);
            return new AssamSearchContext(motifSearchRuntime, motifSearchConfig, query);
        }
    }
}