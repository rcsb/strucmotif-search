package org.rcsb.strucmotif.domain.query;

import org.rcsb.strucmotif.config.MotifPruningStrategy;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.core.KruskalMotifPruner;
import org.rcsb.strucmotif.core.MotifPruner;
import org.rcsb.strucmotif.core.MotifSearchRuntime;
import org.rcsb.strucmotif.core.NoOperationMotifPruner;
import org.rcsb.strucmotif.domain.AtomPairingScheme;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
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
import java.util.Map;
import java.util.Set;

/**
 * The entry point to create {@link MotifSearchQuery} instances.
 */
@Service
public class QueryBuilder {
    private final StructureDataProvider structureDataProvider;
    private final KruskalMotifPruner kruskalMotifPruner;
    private final NoOperationMotifPruner noOperationMotifPruner;
    private final MotifSearchRuntime motifSearchRuntime;
    private final MotifSearchConfig motifSearchConfig;

    @Autowired
    public QueryBuilder(StructureDataProvider structureDataProvider, KruskalMotifPruner kruskalMotifPruner, NoOperationMotifPruner noOperationMotifPruner, MotifSearchRuntime motifSearchRuntime, MotifSearchConfig motifSearchConfig) {
        this.structureDataProvider = structureDataProvider;
        this.kruskalMotifPruner = kruskalMotifPruner;
        this.noOperationMotifPruner = noOperationMotifPruner;
        this.motifSearchRuntime = motifSearchRuntime;
        this.motifSearchConfig = motifSearchConfig;
    }

    /**
     * Define a motif based on the id of the reference structure and a selection of components.
     * @param pdbId the id to acquire
     * @param selection which components to select to define the motif
     * @return mandatory parameter step
     */
    public MandatoryBuilder defineByPdbIdAndSelection(String pdbId, Collection<LabelSelection> selection) {
        Structure structure = structureDataProvider.readSome(new StructureIdentifier(pdbId), selection);
        return defineByStructure(structure);
    }



    /**
     * Define a motif based on a stream of structure data and a selection of components.
     * @param inputStream the data to read
     * @param selection which components to select to define the motif
     * @return mandatory parameter step
     */
    public MandatoryBuilder defineByFileAndSelection(InputStream inputStream, Collection<LabelSelection> selection) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream, selection);
        return defineByStructure(structure);
    }

    /**
     * Routine if structure files contains extracted motif. Checks that the structure contains a reasonable number of
     * components to rule out erroneous arguments.
     * @param structure the file to ready - all components are considered the motif
     * @return mandatory parameter step
     */
    public MandatoryBuilder defineByStructure(Structure structure) {
        long residueCount = structure.getChains()
                .stream()
                .map(Chain::getResidues)
                .mapToLong(Collection::size)
                .sum();

        if (residueCount > motifSearchConfig.getMaxMotifSize()) {
            throw new IllegalArgumentException("maximum motif size is " + motifSearchConfig.getMaxMotifSize() + " - " +
                    "file contains " + residueCount + " residues");
        }

        return new MandatoryBuilder(structure);
    }

    /**
     * Routine if stream of structure data contains extracted motif.
     * @param inputStream the data to ready - all components are considered the motif
     * @return mandatory parameter step
     */
    public MandatoryBuilder defineByFile(InputStream inputStream) {
        Structure structure = structureDataProvider.readFromInputStream(inputStream);
        return defineByStructure(structure);
    }

    /**
     * Parameters are considered mandatory arguments (in the sense that some value has to be given - nonetheless,
     * default values will be used). But internally these values are strictly required. No input validation is performed
     * whatsoever.
     */
    public class MandatoryBuilder {
        private final Structure structure;
        private int backboneDistanceTolerance;
        private int sideChainDistanceTolerance;
        private int angleTolerance;
        private double scoreCutoff;
        private MotifPruner motifPruner;
        private AtomPairingScheme atomPairingScheme;
        private int limit;

        MandatoryBuilder(Structure structure) {
            this.structure = structure;
            this.backboneDistanceTolerance = Parameters.DEFAULT_BACKBONE_DISTANCE_TOLERANCE;
            this.sideChainDistanceTolerance = Parameters.DEFAULT_SIDE_CHAIN_DISTANCE_TOLERANCE;
            this.angleTolerance = Parameters.DEFAULT_ANGLE_TOLERANCE;
            this.scoreCutoff = Parameters.DEFAULT_SCORE_CUTOFF;
            // defines the 'default' motif pruning strategy
            this.motifPruner = QueryBuilder.this.kruskalMotifPruner;
            // defines how to superimpose and score hits
            this.atomPairingScheme = AtomPairingScheme.ALL;
            this.limit = Integer.MAX_VALUE;
        }

        /**
         * Specify the backbone distance tolerance (default: 1).
         * @param backboneDistanceTolerance the tolerance to use
         * @return this builder
         */
        public MandatoryBuilder backboneDistanceTolerance(int backboneDistanceTolerance) {
            this.backboneDistanceTolerance = backboneDistanceTolerance;
            return this;
        }

        /**
         * Specify the side-chain distance tolerance (default: 1).
         * @param sideChainDistanceTolerance the tolerance to use
         * @return this builder
         */
        public MandatoryBuilder sideChainDistanceTolerance(int sideChainDistanceTolerance) {
            this.sideChainDistanceTolerance = sideChainDistanceTolerance;
            return this;
        }

        /**
         * Specify the angle tolerance (default: 1).
         * @param angleTolerance the tolerance to use
         * @return this builder
         */
        public MandatoryBuilder angleTolerance(int angleTolerance) {
            this.angleTolerance = angleTolerance;
            return this;
        }

        /**
         * When to filter hits by some score (use 0 or some high value to don't filter at all). This is valuable to ease
         * the memory consumption (eliminated hits are not stored) and network traffic (eliminated hits are not
         * propagated to the result list or some front-end). Default: 3.0.
         * @param scoreCutoff the score cutoff above which hits are filtered
         * @return this builder
         */
        public MandatoryBuilder scoreCutoff(double scoreCutoff) {
            this.scoreCutoff = scoreCutoff;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruner the implementation to prune motifs
         * @return this builder
         */
        public MandatoryBuilder motifPruningStrategy(MotifPruner motifPruner) {
            this.motifPruner = motifPruner;
            return this;
        }

        /**
         * Specify the motif pruning strategy.
         * @param motifPruningStrategy the strategy to prune motifs
         * @return this builder
         */
        public MandatoryBuilder motifPruningStrategy(MotifPruningStrategy motifPruningStrategy) {
            switch (motifPruningStrategy) {
                case KRUSKAL:
                    this.motifPruner = QueryBuilder.this.kruskalMotifPruner;
                    break;
                case NONE:
                    this.motifPruner = QueryBuilder.this.noOperationMotifPruner;
                    break;
                default:
                    throw new UnsupportedOperationException("Unhandled case: " + motifPruningStrategy);
            }
            return this;
        }

        /**
         * Specify the atom pairing strategy.
         * @param atomPairingScheme the strategy to pair atoms during alignment/scoring
         * @return this builder
         */
        public MandatoryBuilder atomPairingScheme(AtomPairingScheme atomPairingScheme) {
            this.atomPairingScheme = atomPairingScheme;
            return this;
        }

        /**
         * Stop after a certain number of accepted hits.
         * @param limit the maximum number of hits below the RMSD threshold
         * @return this builder
         */
        public MandatoryBuilder limitResults(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Creates a {@link Parameters} instance based on all values. Proceeds to the next step.
         * @return the optional argument step
         */
        public OptionalStepBuilder buildParameters() {
            Parameters parameters = new Parameters(backboneDistanceTolerance,
                    sideChainDistanceTolerance,
                    angleTolerance,
                    scoreCutoff,
                    motifPruner,
                    atomPairingScheme,
                    limit);
            return new OptionalStepBuilder(structure, parameters);
        }
    }

    /**
     * Optional parameters of the algorithm.
     */
    public class OptionalStepBuilder {
        private final Structure structure;
        private final Parameters parameters;
        private final Map<LabelSelection, Set<ResidueType>> exchanges;
        private final Set<StructureIdentifier> whitelist;
        private final Set<StructureIdentifier> blacklist;

        OptionalStepBuilder(Structure structure, Parameters parameters) {
            this.structure = structure;
            this.parameters = parameters;
            this.exchanges = new HashMap<>();
            this.whitelist = new HashSet<>();
            this.blacklist = new HashSet<>();
        }

        /**
         * Register an exchange by specifying a 'range'/collection of component types.
         * @param labelSelection the position referenced
         * @param residueTypes all allowed types
         * @return this builder
         */
        public OptionalStepBuilder addPositionSpecificExchange(LabelSelection labelSelection, Collection<ResidueType> residueTypes) {
            Set<ResidueType> exchange = exchanges.computeIfAbsent(labelSelection, k -> new LinkedHashSet<>());
            exchange.addAll(residueTypes);
            return this;
        }

        public OptionalStepBuilder whitelist(Collection<StructureIdentifier> structureIdentifiers) {
            this.whitelist.addAll(structureIdentifiers);
            return this;
        }

        public OptionalStepBuilder blacklist(Collection<StructureIdentifier> structureIdentifiers) {
            this.blacklist.addAll(structureIdentifiers);
            return this;
        }

        /**
         * Build the actual container.
         * @return the immutable instance of all query parameters
         */
        public MotifSearchQuery buildQuery() {
            return new MotifSearchQuery(motifSearchRuntime, structure, parameters, exchanges, whitelist, blacklist);
        }
    }
}
