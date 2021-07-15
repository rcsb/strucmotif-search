package org.rcsb.strucmotif.io.read;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.AtomSite;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.cif.schema.mm.PdbxStructAssemblyGen;
import org.rcsb.cif.schema.mm.PdbxStructOperList;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * The default implementation of a structure reader.
 */
@Service
public class StructureReaderImpl implements StructureReader {
    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection) {
        try {
            MmCifFile mmCifFile = CifIO.readFromInputStream(inputStream).as(StandardSchemata.MMCIF);
            return new StructureReaderState(mmCifFile, selection).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class StructureReaderState {
        private static final Pattern LIST = Pattern.compile(",");

        // all relevant categories
        private final StructureIdentifier structureIdentifier;
        private final AtomSite atomSite;
        private final PdbxStructAssemblyGen pdbxStructAssemblyGen;
        private final PdbxStructOperList pdbxStructOperList;

        // handles to binary data array in atom site
        private final String[] labelAtomId;
        private final String[] labelCompId;
        private final String[] labelAsymIds;
        private final int[] labelSeqIds;
        private final double[] cartnX;
        private final double[] cartnY;
        private final double[] cartnZ;

        // used during scoring of hits: obtain specific set of entities
        private final Set<String> selectedAssemblies;
        private final Collection<? extends ResidueSelection> selection;

        // the 'state'
        private ResidueIdentifier currentResidueIdentifier;
        private List<Atom> atomBuffer;
        private String currentChain;
        private List<Residue> residueBuffer;
        private final List<Pair<ChainIdentifier, List<Residue>>> chains;

        /**
         * Initialize a new reading operation.
         * @param mmCifFile data source in binary format
         * @param selection optional selection of residues (may be null)
         */
        private StructureReaderState(MmCifFile mmCifFile, Collection<? extends ResidueSelection> selection) {
            MmCifBlock block = mmCifFile.getFirstBlock();

            this.structureIdentifier = new StructureIdentifier(block.getBlockHeader().toLowerCase());
            // TODO 1car is empty - address this (either by writing better files or pass a real map instance to the BinaryBlock constructor in ciftools
            this.atomSite = block.getAtomSite();
            this.pdbxStructAssemblyGen = block.getPdbxStructAssemblyGen();
            this.pdbxStructOperList = block.getPdbxStructOperList();

            this.labelAtomId = atomSite.getLabelAtomId().getArray();
            this.labelCompId = atomSite.getLabelCompId().getArray();
            this.labelAsymIds = atomSite.getLabelAsymId().getArray();
            this.labelSeqIds = atomSite.getLabelSeqId().getArray();
            this.cartnX = atomSite.getCartnX().getArray();
            this.cartnY = atomSite.getCartnY().getArray();
            this.cartnZ = atomSite.getCartnZ().getArray();

            this.currentChain = null;
            this.currentResidueIdentifier = null;

            if (selection == null) {
                this.selectedAssemblies = null;
                this.atomBuffer = new ArrayList<>(20);
                this.residueBuffer = new ArrayList<>(100);
            } else {
                this.selectedAssemblies = selection.stream()
                        .map(ResidueSelection::getStructOperId)
                        .collect(Collectors.toSet());
                this.atomBuffer = new ArrayList<>(20);
                this.residueBuffer = new ArrayList<>(selection.size());
            }
            this.chains = new ArrayList<>();
            this.selection = selection;
        }

        private Structure build() {
            int atomId = 0;

            // keep track of fulfilled selections - if all present, break loop to save time
            int fulfilledCount = 0;
            boolean fulfilled = false;
            int target = selection == null ? 0 : selection.size();

            String lastMatchedChainId = null;
            String lastChainId = null;
            int lastMatchedSeqId = -1;
            int lastSeqId = -1;
            int residueIndex = -1;

            for (int row = 0; row < atomSite.getRowCount(); row++) {
                int labelSeqId = labelSeqIds[row];
                String labelAsymId = labelAsymIds[row];
                if (!labelAsymId.equals(lastChainId) || labelSeqId != lastSeqId) {
                    residueIndex++;
                    lastChainId = labelAsymId;
                    lastSeqId = labelSeqId;
                }

                if (target > 0) {
                    boolean match = false;
                    for (ResidueSelection selector : selection) {
                        if (selector.test(labelAsymId, labelSeqId, residueIndex)) {
                            match = true;

                            if (!labelAsymId.equals(lastMatchedChainId) || labelSeqId != lastMatchedSeqId) {
                                lastMatchedChainId = labelAsymId;
                                lastMatchedSeqId = labelSeqId;
                                fulfilledCount++;
                                if (fulfilledCount == target) {
                                    fulfilled = true;
                                }
                            }
                            break;
                        }
                    }

                    // give option to break loop if all components were found
                    if (fulfilled && (!labelAsymId.equals(lastMatchedChainId) || labelSeqId != lastMatchedSeqId)) {
                        break;
                    }

                    // if not found - don't bother creating object
                    if (!match) {
                        continue;
                    }
                }

                // handle atom level
                double[] coord = new double[] {
                        cartnX[row],
                        cartnY[row],
                        cartnZ[row]
                };
                AtomIdentifier atomIdentifier = new AtomIdentifier(labelAtomId[row], ++atomId);
                Atom atom = StructureFactory.createAtom(atomIdentifier, coord);

                boolean chainChange = !labelAsymId.equals(currentChain);

                // handle entity level
                ResidueIdentifier residueIdentifier = new ResidueIdentifier(labelCompId[row], labelSeqId, residueIndex);
                // we have to update the entity if the chain changed
                if (chainChange || !residueIdentifier.equals(currentResidueIdentifier)) {
                    atomBuffer = addResidue();
                    currentResidueIdentifier = residueIdentifier;
                }

                // handle chain level
                if (chainChange) {
                    residueBuffer = addChain();
                    currentChain = labelAsymId;
                }

                if (atomBuffer.stream().noneMatch(a -> a.getAtomIdentifier().describeSameAtom(atomIdentifier))) {
                    atomBuffer.add(atom);
                }
            }

            addResidue();
            addChain();

            return StructureFactory.createStructure(structureIdentifier, buildAssemblies(chains, selection));
        }

        /**
         * Processes all 'buffered' atoms and creates a residue from it. Clears the buffer.
         * @return the cleared buffer
         */
        private List<Atom> addResidue() {
            if (atomBuffer.size() > 0) {
                residueBuffer.add(StructureFactory.createResidue(currentResidueIdentifier,
                        atomBuffer,
                        Transformation.IDENTITY_MATRIX_4D));
                return new ArrayList<>();
            }
            return atomBuffer;
        }

        /**
         * Processes all 'buffered' residues and creates a chain from it. Clears the buffer.
         * @return the cleared buffer
         */
        private List<Residue> addChain() {
            if (residueBuffer.size() > 0) {
                chains.add(new Pair<>(new ChainIdentifier(currentChain, "1"), residueBuffer));
                return new ArrayList<>();
            }
            return residueBuffer;
        }

        private static final Pattern OPERATION_PATTERN = Pattern.compile("\\)\\(");
        private Map<String, double[][]> getTransformations(Map<String, double[][]> transformations, String operations) {
            Map<String, double[][]> composedTransformations = new LinkedHashMap<>();

            String[] split = OPERATION_PATTERN.split(operations);
            if (split.length > 1) {
                List<String> ids1 = extractTransformationIds(split[0]);
                List<String> ids2 = extractTransformationIds(split[1]);
                for (String id1 : ids1) {
                    for (String id2 : ids2) {
                        composedTransformations.put(id1 + "x" + id2, multiply4d(transformations.get(id1), transformations.get(id2)));
                    }
                }
            } else {
                List<String> ids = extractTransformationIds(operations);
                for (String id : ids) {
                    composedTransformations.put(id, transformations.get(id));
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
         * Construct bioassemblies from parsed chains and registered operations.
         * @param asymChains 'raw' chains - mere mapping between identifiers and all components
         * @param selection nullable selection of residues - need this to omit duplicates when residues in non-identity chains are selected
         * @return all constructed chains
         */
        private List<Chain> buildAssemblies(List<Pair<ChainIdentifier, List<Residue>>> asymChains, Collection<? extends ResidueSelection> selection) {
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
            if (pdbxStructAssemblyGen.isDefined()) {
                for (int row = 0; row < pdbxStructAssemblyGen.getRowCount(); row++) {
                    String operExpression = pdbxStructAssemblyGen.getOperExpression().get(row);
                    List<String> asymIds = LIST.splitAsStream(pdbxStructAssemblyGen.getAsymIdList().get(row))
                            .collect(Collectors.toList());

                    Map<String, double[][]> transformations = getTransformations(matrices, operExpression);
                    for (Map.Entry<String, double[][]> transformation : transformations.entrySet()) {
                        for (String asymId : asymIds) {
                            if (row > 0 && coveredAsymIds.contains(asymId)) {
                                continue;
                            }

                            String operKey = transformation.getKey();
                            if (selectedAssemblies != null && selectedAssemblies.size() > 0 && !selectedAssemblies.contains(operKey)) {
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

                            ChainIdentifier chainIdentifier = new ChainIdentifier(originalChain.getFirst().getLabelAsymId(), operKey);
                            chains.add(StructureFactory.createChain(chainIdentifier,
                                    filter(chainIdentifier, originalChain.getSecond(), selection),
                                    transformation.getValue()));
                            coveredAsymIds.add(asymId);
                        }
                    }
                }
            } else {
                // nothing defined explicitly
                chains = asymChains.stream()
                        .map(pair -> StructureFactory.createChain(pair.getFirst(), pair.getSecond(), Transformation.IDENTITY_MATRIX_4D))
                        .collect(Collectors.toList());
            }

            return chains;
        }

        private List<Residue> filter(ChainIdentifier chainIdentifier, List<Residue> raw, Collection<? extends ResidueSelection> selection) {
            if (selection == null) {
                return raw;
            } else {
                // must be correct struct_oper_id
                Collection<? extends ResidueSelection> subselection = selection.stream()
                        .filter(s -> s.getStructOperId().equals(chainIdentifier.getStructOperId()))
                        .collect(Collectors.toList());
                return raw.stream()
                        // must match residue-specific props (label_asym_id, label_seq_id, index)
                        .filter(residue -> subselection.stream()
                                .anyMatch(s -> s.test(chainIdentifier.getLabelAsymId(),
                                                residue.getResidueIdentifier().getLabelSeqId(),
                                                residue.getResidueIdentifier().getIndex())))
                        .collect(Collectors.toList());
            }
        }
    }
}
