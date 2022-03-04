package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.core.IllegalQueryDefinitionException;
import org.rcsb.strucmotif.core.KruskalMotifPruner;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.core.NoOperationMotifPruner;
import org.rcsb.strucmotif.domain.StructureSearchContext;
import org.rcsb.strucmotif.domain.align.AtomPairingScheme;
import org.rcsb.strucmotif.domain.motif.MotifDefinition;
import org.rcsb.strucmotif.domain.structure.LabelAtomId;
import org.rcsb.strucmotif.domain.structure.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.InvertedIndex;
import org.rcsb.strucmotif.io.StructureDataProvider;
import org.rcsb.strucmotif.io.StructureIndexProvider;
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
 * The entry point to create {@link StructureSearchContext} instances.
 */
@Service
public class StructureContextBuilder implements ContextBuilder<StructureContextBuilder.MandatoryBuilderStep, StructureSearchContext> {
    private final StructureIndexProvider structureIndexProvider;
    private final StructureDataProvider structureDataProvider;
    private final KruskalMotifPruner kruskalMotifPruner;
    private final NoOperationMotifPruner noOperationMotifPruner;
    private final MotifSearchRuntime motifSearchRuntime;
    private final StrucmotifConfig strucmotifConfig;
    private final InvertedIndex invertedIndex;

    /**
     * Construct a new query builder.
     * @param structureIndexProvider injectable structure index provider
     * @param structureDataProvider injectable structure data provider
     * @param kruskalMotifPruner injectable motif pruner
     * @param noOperationMotifPruner injectable nop motif pruner
     * @param motifSearchRuntime injectable runtime
     * @param strucmotifConfig injectable config
     * @param invertedIndex injectable inverted index
     */
    @Autowired
    public StructureContextBuilder(StructureIndexProvider structureIndexProvider, StructureDataProvider structureDataProvider, KruskalMotifPruner kruskalMotifPruner, NoOperationMotifPruner noOperationMotifPruner, MotifSearchRuntime motifSearchRuntime, StrucmotifConfig strucmotifConfig, InvertedIndex invertedIndex) {
        this.structureIndexProvider = structureIndexProvider;
        this.structureDataProvider = structureDataProvider;
        this.kruskalMotifPruner = kruskalMotifPruner;
        this.noOperationMotifPruner = noOperationMotifPruner;
        this.motifSearchRuntime = motifSearchRuntime;
        this.strucmotifConfig = strucmotifConfig;
        this.invertedIndex = invertedIndex;
    }

    /**
     * Define a motif based on the id of the reference structure and a selection of components.
     * @param structureIdentifier the id to acquire
     * @param selection which components to select to define the motif
     * @return mandatory parameter step
     * @throws IllegalQueryDefinitionException if chains/residues aren't found or if distance constraints are violated
     */
    public MandatoryBuilderStep defineByPdbIdAndSelection(String structureIdentifier, List<LabelSelection> selection) {
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
    public MandatoryBuilderStep defineByFileAndSelection(InputStream inputStream, List<LabelSelection> selection) {
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
    public MandatoryBuilderStep defineByStructureAndSelection(Structure structure, List<LabelSelection> labelSelections) {
        try {
            List<Map<LabelAtomId, float[]>> residues = structure.manifestResidues(labelSelections);

            if (residues.size() > strucmotifConfig.getMaxMotifSize()) {
                throw new IllegalArgumentException("maximum motif size is " + strucmotifConfig.getMaxMotifSize() + " - " +
                        "file contains " + residues.size() + " residues");
            }

            String structureIdentifier = structure.getStructureIdentifier().toUpperCase();
            return new MandatoryBuilderStep(structureIdentifier, structure, labelSelections, residues);
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
    public MandatoryBuilderStep defineByFile(InputStream inputStream) {
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
    public MandatoryBuilderStep defineByMotif(MotifDefinition motifDefinition) {
        return defineByPdbIdAndSelection(motifDefinition.getStructureIdentifier(), motifDefinition.getLabelSelections())
                .propagateExchanges(motifDefinition.getPositionSpecificExchanges());
    }

    /**
     * Builder for everything that must be set (but might fall back to default values).
     */
    public class MandatoryBuilderStep implements ContextBuilder.MandatoryBuilder<MandatoryBuilderStep, StructureSearchContext> {
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

        MandatoryBuilderStep(String structureIdentifier, Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues) {
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
            this.motifPruner = StructureContextBuilder.this.kruskalMotifPruner;
            this.limit = Integer.MAX_VALUE;
            this.undefinedAssemblies = false;
        }

        @Override
        public MandatoryBuilderStep backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        @Override
        public MandatoryBuilderStep sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        @Override
        public MandatoryBuilderStep angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        @Override
        public MandatoryBuilderStep rmsdCutoff(double rmsdCutoff) {
            this.rmsdCutoff = (float) rmsdCutoff;
            return this;
        }

        @Override
        public MandatoryBuilderStep atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        @Override
        public MandatoryBuilderStep motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        @Override
        public MandatoryBuilderStep motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
            switch (motifPruningStrategy) {
                case KRUSKAL:
                    this.motifPruner = StructureContextBuilder.this.kruskalMotifPruner;
                    break;
                case NONE:
                    this.motifPruner = StructureContextBuilder.this.noOperationMotifPruner;
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
        public MandatoryBuilderStep limitResults(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Return undefined assemblies (if indexed).
         * @param undefinedAssemblies a boolean
         * @return this builder
         */
        public MandatoryBuilderStep undefinedAssemblies(boolean undefinedAssemblies) {
            this.undefinedAssemblies = undefinedAssemblies;
            return this;
        }

        /**
         * Allow to set downstream exchanges via upstream definition.
         * @param upstreamExchanges exchanges from a motif definition
         * @return this builder
         */
        MandatoryBuilderStep propagateExchanges(Set<PositionSpecificExchange> upstreamExchanges) {
            this.upstreamExchanges = upstreamExchanges;
            return this;
        }

        public OptionalBuilderStep buildParameters() {
            StructureParameters parameters = new StructureParameters(backboneDistanceTolerance,
                    sideChainDistanceTolerance,
                    angleTolerance,
                    rmsdCutoff,
                    atomPairingScheme,
                    motifPruner,
                    limit,
                    undefinedAssemblies);
            return new OptionalBuilderStep(structureIdentifier, structure, labelSelections, residues, parameters, upstreamExchanges);
        }
    }

    /**
     * Optional parameters of the algorithm.
     */
    public class OptionalBuilderStep implements OptionalBuilder<StructureSearchContext> {
        private final String structureIdentifier;
        private final Structure structure;
        private final List<LabelSelection> labelSelections;
        private final List<Map<LabelAtomId, float[]>> residues;
        private final StructureParameters parameters;
        private final Map<LabelSelection, Set<ResidueType>> exchanges;
        private final Set<String> whitelist;
        private final Set<String> blacklist;
        private StructureDeterminationMethodology structureDeterminationMethodology;

        OptionalBuilderStep(String structureIdentifier, Structure structure, List<LabelSelection> labelSelections, List<Map<LabelAtomId, float[]>> residues, StructureParameters parameters, Set<PositionSpecificExchange> upstreamExchanges) {
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
        public OptionalBuilderStep addPositionSpecificExchange(LabelSelection labelSelection, Collection<ResidueType> residueTypes) {
            Set<ResidueType> exchange = exchanges.computeIfAbsent(labelSelection, k -> new LinkedHashSet<>());
            exchange.addAll(residueTypes);
            return this;
        }

        /**
         * Narrow down the search space to the specified entries. Leave empty to search everything.
         * @param structureIdentifiers a collection of structure identifiers
         * @return this builder
         */
        public OptionalBuilderStep whitelist(Collection<String> structureIdentifiers) {
            this.whitelist.addAll(structureIdentifiers);
            return this;
        }

        /**
         * Filter out hits from the specified entries.
         * @param structureIdentifiers a collection of structure identifiers
         * @return this builder
         */
        public OptionalBuilderStep blacklist(Collection<String> structureIdentifiers) {
            this.blacklist.addAll(structureIdentifiers);
            return this;
        }

        /**
         * Narrow down a search to a specific target set.
         * @param structureDeterminationMethodology the search space of choice
         * @return this builder
         */
        public OptionalBuilderStep structureDeterminationMethodology(StructureDeterminationMethodology structureDeterminationMethodology) {
            this.structureDeterminationMethodology = structureDeterminationMethodology;
            return this;
        }

        @Override
        public StructureSearchContext buildContext() {
            StructureQuery query = new StructureQuery(structureIdentifier,
                    structure,
                    labelSelections,
                    residues,
                    parameters,
                    exchanges,
                    whitelist,
                    blacklist,
                    structureDeterminationMethodology,
                    strucmotifConfig);
            return new StructureSearchContext(motifSearchRuntime, strucmotifConfig, invertedIndex, structureIndexProvider, structureDataProvider, query);
        }
    }
}
