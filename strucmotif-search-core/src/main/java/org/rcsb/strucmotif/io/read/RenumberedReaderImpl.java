package org.rcsb.strucmotif.io.read;

import com.google.inject.Singleton;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.CifOptions;
import org.rcsb.cif.model.binary.BinaryFile;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A dedicated structure reader implementation for renumbered files (which will yield and be accessible by {@link
 * IndexSelection} in a consistent way).
 */
@Singleton
public class RenumberedReaderImpl implements RenumberedReader {
    private static final CifOptions OPTIONS = CifOptions.builder().fileFormatHint(CifOptions.CifOptionsBuilder.FileFormat.BCIF_PLAIN).build();

    @Override
    public Structure readById(String pdbId, Collection<IndexSelection> selection) {
        Path path = MotifSearch.ARCHIVE_PATH.resolve(pdbId + ".bcif");
        try {
            BinaryFile cifFile = (BinaryFile) CifIO.readFromPath(path, OPTIONS);
            return new RenumberedReaderState(cifFile, selection).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        return readFromInputStream(inputStream, null);
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<IndexSelection> selection) {
        try {
            BinaryFile cifFile = (BinaryFile) CifIO.readFromInputStream(inputStream, OPTIONS);
            return new RenumberedReaderState(cifFile, selection).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class RenumberedReaderState extends AbstractReaderState<IndexSelection> {
        RenumberedReaderState(BinaryFile binaryCifFile, Collection<IndexSelection> selection) {
            super(binaryCifFile, selection);
        }

        @SuppressWarnings("Duplicates")
        public Structure build() {
            int atomId = 0;

            // keep track of fulfilled selections - if all present, bail out to save time
            int fulfilledCount = 0;
            boolean fulfilled = false;
            int target = selectors == null ? 0 : selectors.size();

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
                    for (IndexSelection selector : selectors) {
                        if (selector.getIndex() == residueIndex) {
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

                ChainIdentifier chainIdentifier = new ChainIdentifier(labelAsymId, 1);
                boolean chainChange = !chainIdentifier.equals(currentChainIdentifier);

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
                    currentChainIdentifier = chainIdentifier;
                }

                if (atomBuffer.stream().noneMatch(a -> a.getAtomIdentifier().describeSameAtom(atomIdentifier))) {
                    atomBuffer.add(atom);
                }
            }

            addResidue();
            addChain();

            return StructureFactory.createStructure(structureIdentifier, title, buildAssembly(chains));
        }
    }
}
