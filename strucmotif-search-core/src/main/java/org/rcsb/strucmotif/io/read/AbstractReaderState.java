package org.rcsb.strucmotif.io.read;

import org.rcsb.cif.model.binary.BinaryFile;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.Selection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.StructureFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.rcsb.strucmotif.math.Algebra.multiply4d;

/**
 * Shared code of all reader implementations.
 */
abstract class AbstractReaderState<S extends Selection> {
    private static final Pattern LIST = Pattern.compile(",");

    // all relevant categories
    final StructureIdentifier structureIdentifier;
    final String title;
    final AtomSite atomSite;
    private final PdbxStructAssemblyGen pdbxStructAssemblyGen;
    private final PdbxStructOperList pdbxStructOperList;

    // handles to binary data array in atom site
    final String[] labelAtomId;
    final String[] labelCompId;
    final String[] labelAsymIds;
    final int[] labelSeqIds;
    final double[] cartnX;
    final double[] cartnY;
    final double[] cartnZ;

    // used during scoring of hits: obtain specific set of entities
    private final Set<Integer> selectedAssemblies;
    final Collection<S> selectors;

    // the 'state'
    ResidueIdentifier currentResidueIdentifier;
    List<Atom> atomBuffer;
    ChainIdentifier currentChainIdentifier;
    List<Residue> residueBuffer;
    final List<Pair<ChainIdentifier, List<Residue>>> chains;

    /**
     * Initialize a new reading operation.
     * @param cifFile data source in binary format
     * @param selection optional selection of components (may be null)
     */
    AbstractReaderState(BinaryFile cifFile, Collection<S> selection) {
        MmCifBlock block = cifFile.as(StandardSchemata.MMCIF).getFirstBlock();

        this.structureIdentifier = new StructureIdentifier(block.getBlockHeader().toLowerCase());
        this.title = tryToGetTitle(block);
        this.atomSite = block.getAtomSite();
        this.pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
        this.pdbxStructOperList = block.getPdbxStructOperList();

        // readers are specifically tied to binary files and use the API in a really unsafe way
        this.labelAtomId = atomSite.getLabelAtomId().getArray();
        this.labelCompId = atomSite.getLabelCompId().getArray();
        this.labelAsymIds = atomSite.getLabelAsymId().getArray();
        this.labelSeqIds = atomSite.getLabelSeqId().getArray();
        this.cartnX = atomSite.getCartnX().getArray();
        this.cartnY = atomSite.getCartnY().getArray();
        this.cartnZ = atomSite.getCartnZ().getArray();

        this.currentChainIdentifier = null;
        this.currentResidueIdentifier = null;

        if (selection == null) {
            this.selectedAssemblies = null;
            this.atomBuffer = new ArrayList<>(20);
            this.residueBuffer = new ArrayList<>(100);
        } else {
            this.selectedAssemblies = selection.stream()
                .map(Selection::getAssemblyId)
                .collect(Collectors.toSet());
            this.atomBuffer = new ArrayList<>(20);
            this.residueBuffer = new ArrayList<>(selection.size());
        }
        this.chains = new ArrayList<>();
        this.selectors = selection;
    }

    private String tryToGetTitle(MmCifBlock block) {
        try {
            return block.getStruct().getTitle().get(0).toLowerCase();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * Processes all 'buffered' atoms and creates a residue from it. Clears the buffer.
     * @return the cleared buffer
     */
    List<Atom> addResidue() {
        if (atomBuffer.size() > 0) {
            residueBuffer.add(StructureFactory.createResidue(currentResidueIdentifier,
                    atomBuffer,
                    Matrix4DTransformation.IDENTITY_MATRIX_4D));
            return new ArrayList<>();
        }
        return atomBuffer;
    }

    /**
     * Processes all 'buffered' residues and creates a chain from it. Clears the buffer.
     * @return the cleared buffer
     */
    List<Residue> addChain() {
        if (residueBuffer.size() > 0) {
            chains.add(new Pair<>(currentChainIdentifier, residueBuffer));
            return new ArrayList<>();
        }
        return residueBuffer;
    }

    private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
    private List<double[][]> getTransformations(Map<String, double[][]> transformations, String operations) {
        List<double[][]> composedTransformations = new ArrayList<>();

        String[] split = OPERATION_PATTERN.split(operations);
        if (split.length > 1) {
            List<String> ids1 = extractTransformationIds(split[0]);
            List<String> ids2 = extractTransformationIds(split[1]);
            for (String id1 : ids1) {
                for (String id2 : ids2) {
                    composedTransformations.add(multiply4d(transformations.get(id1), transformations.get(id2)));
                }
            }
        } else {
            List<String> ids = extractTransformationIds(operations);
            for (String id : ids) {
                composedTransformations.add(transformations.get(id));
            }
        }

        return composedTransformations;
    }

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private List<String> extractTransformationIds(String rawOperation) {
        String prepared = rawOperation.replace("(", "")
                .replace(")", "")
                .replace("'", "");

        return COMMA_PATTERN.splitAsStream(prepared)
                .flatMap(this::extractTransformationRanges)
                .collect(Collectors.toList());
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("-");
    private Stream<String> extractTransformationRanges(String raw) {
        String[] s = RANGE_PATTERN.split(raw);
        if (s.length == 1) {
            return Stream.of(raw);
        } else {
            return IntStream.range(Integer.parseInt(s[0]), Integer.parseInt(s[1]) + 1)
                    .mapToObj(String::valueOf);
        }
    }

    /**
     * Construct a new residue identifier.
     * @param row source of info in the structure file
     * @param residueIndex the current residue index
     * @return the instance
     */
    ResidueIdentifier createResidueIdentifier(int row, int residueIndex) {
        String labelCompId = this.labelCompId[row];
        int labelSeqId = labelSeqIds[row];

        return new ResidueIdentifier(labelCompId, labelSeqId, residueIndex);
    }

    /**
     * Construct a new atom identifier.
     * @param id index of the atom
     * @param atomId the atom name
     * @param altLoc the alt location label - empty string for none
     * @return the instance
     */
    AtomIdentifier createAtomIdentifier(int id, String atomId, String altLoc) {
        return altLoc.isEmpty() ? new AtomIdentifier(atomId, id) : new AtomIdentifier(atomId, id, altLoc);
    }

    /**
     * Construct bioassembly from parsed chains and registered operations.
     * @param asymChains 'raw' chains - mere mapping between identifiers and all components
     * @return all constructed chains
     */
    List<Chain> buildAssembly(List<Pair<ChainIdentifier, List<Residue>>> asymChains) {
        List<Chain> chains = new ArrayList<>();
        Map<String, double[][]> matrices = IntStream.range(0, pdbxStructOperList.getRowCount())
                .boxed()
                .collect(Collectors.toMap(row -> pdbxStructOperList.getId().get(row),
                        row -> new double[][] {
                                { pdbxStructOperList.getMatrix11().get(row), pdbxStructOperList.getMatrix12().get(row),
                                        pdbxStructOperList.getMatrix13().get(row), pdbxStructOperList.getVector1().get(row) },
                                { pdbxStructOperList.getMatrix21().get(row), pdbxStructOperList.getMatrix22().get(row),
                                        pdbxStructOperList.getMatrix23().get(row), pdbxStructOperList.getVector2().get(row) },
                                { pdbxStructOperList.getMatrix31().get(row), pdbxStructOperList.getMatrix32().get(row),
                                        pdbxStructOperList.getMatrix33().get(row), pdbxStructOperList.getVector3().get(row) },
                                { 0, 0, 0, 1 }
                        }));

        // use set to ensure chains get only moved to one bioassembly - usually the first one should cover all
        // chains - in cases such as 2y5b, chains are transformed by 2 distinct operations
        Set<String> coveredAsymIds = new HashSet<>();
        if (pdbxStructAssemblyGen.isDefined() && pdbxStructAssemblyGen.getRowCount() > 0) {
            for (int row = 0; row < pdbxStructAssemblyGen.getRowCount(); row++) {
                int assemblyId = 0;
                String operExpression = pdbxStructAssemblyGen.getOperExpression().get(row);
                List<String> asymIds = LIST.splitAsStream(pdbxStructAssemblyGen.getAsymIdList().get(row))
                        .collect(Collectors.toList());

                List<double[][]> transformations = getTransformations(matrices, operExpression);
                for (double[][] transformation : transformations) {
                    assemblyId++;

                    for (String asymId : asymIds) {
                        if (row > 0 && coveredAsymIds.contains(asymId)) {
                            continue;
                        }

                        if (selectedAssemblies != null && selectedAssemblies.size() > 0 && !selectedAssemblies.contains(assemblyId)) {
                            continue;
                        }

                        Optional<Pair<ChainIdentifier, List<Residue>>> originalChainOptional = asymChains.stream()
                                .filter(pair -> pair.getFirst().getLabelAsymId().equals(asymId))
                                .findFirst();

                        // happens for non-polymer chains
                        if (originalChainOptional.isEmpty()) {
                            continue;
                        }
                        Pair<ChainIdentifier, List<Residue>> originalChain = originalChainOptional.get();

                        chains.add(StructureFactory.createChain(new ChainIdentifier(originalChain.getFirst().getLabelAsymId(),
                                        assemblyId),
                                originalChain.getSecond(),
                                transformation));
                        coveredAsymIds.add(asymId);
                    }
                }
            }
        } else {
            // nothing defined explicitly
            chains = asymChains.stream()
                    .map(pair -> StructureFactory.createChain(pair.getFirst(), pair.getSecond(), Matrix4DTransformation.IDENTITY_MATRIX_4D))
                    .collect(Collectors.toList());
        }

        return chains;
    }
}
