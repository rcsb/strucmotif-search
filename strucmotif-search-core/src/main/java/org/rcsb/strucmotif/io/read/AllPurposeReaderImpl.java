package org.rcsb.strucmotif.io.read;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.CifOptions;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.IntColumn;
import org.rcsb.cif.model.binary.BinaryFile;
import org.rcsb.cif.model.text.TextFile;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;

/**
 * An all-purpose reader structure reader. Used to read the original binary files as well as user input.
 */
@Service
public class AllPurposeReaderImpl implements AllPurposeReader {
    private static final Logger logger = LoggerFactory.getLogger(AllPurposeReaderImpl.class);
    private static final String FETCH_URL = "https://models.rcsb.org/%s.bcif";

    @Override
    public Structure readById(StructureIdentifier structureIdentifier, Collection<LabelSelection> selection) {
        try {
            URL url = new URL(String.format(FETCH_URL, structureIdentifier.getPdbId()));
            logger.debug("Loading structure from {} - selection: {}", url, selection);
            InputStream inputStream = url.openStream();
            BinaryFile cifFile = (BinaryFile) CifIO.readFromInputStream(inputStream);
            return new GenericReaderState(cifFile, selection).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream) {
        return readFromInputStream(inputStream, null);
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<LabelSelection> selection) {
        try {
            // user can upload text files, parsing depends on binary files - this is a hacky solution for that
            CifFile file = CifIO.readFromInputStream(inputStream);
            if (file instanceof TextFile) {
                file = CifIO.readFromInputStream(new ByteArrayInputStream(CifIO.writeBinary(file)));
            }

            BinaryFile cifFile = (BinaryFile) file;
            return new GenericReaderState(cifFile, selection).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static class GenericReaderState extends AbstractReaderState<LabelSelection> {
        GenericReaderState(BinaryFile binaryCifFile, Collection<LabelSelection> selection) {
            super(binaryCifFile, selection);
        }

        @SuppressWarnings("Duplicates")
        public Structure build() {
            // determine the first position which invalidates the first model
            final int firstModelEnd = determineFirstModelEnd();
            String lastChainId = null;
            int lastSeqId = -1;
            int residueIndex = -1;

            for (int row = 0; row < firstModelEnd; row++) {
                String labelAsymId = labelAsymIds[row];
                int labelSeqId = labelSeqIds[row];
                if (!labelAsymId.equals(lastChainId) || labelSeqId != lastSeqId) {
                    residueIndex++;
                    lastChainId = labelAsymId;
                    lastSeqId = labelSeqId;
                }

                if (selectors != null && selectors.size() > 0) {
                    boolean match = false;
                    for (LabelSelection labelSelection : selectors) {
                        if (labelSelection.getLabelAsymId().equals(labelAsymId) &&
                                labelSelection.getLabelSeqId() == labelSeqId) {
                            match = true;
                            break;
                        }
                    }

                    if (!match) {
                        continue;
                    }
                } else {
                    // skip non-polymer entities
                    if (labelSeqId == 0) {
                        continue;
                    }
                }

                // handle atom level
                double[] coord = new double[] {
                        cartnX[row],
                        cartnY[row],
                        cartnZ[row]
                };

                AtomIdentifier atomIdentifier = createAtomIdentifier(atomSite.getId().get(row), labelAtomId[row], atomSite.getLabelAltId().get(row));
                Atom atom = StructureFactory.createAtom(atomIdentifier, coord);

                boolean chainChange = !labelAsymId.equals(currentChain);

                // handle entity level
                ResidueIdentifier residueIdentifier = createResidueIdentifier(row, residueIndex);
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

            return StructureFactory.createStructure(structureIdentifier, buildAssembly(chains));
        }

        private int determineFirstModelEnd() {
            IntColumn pdbxPDBModelNum = atomSite.getPdbxPDBModelNum();
            if (pdbxPDBModelNum.isDefined()) {
                final int[] modelNumbers = pdbxPDBModelNum.getArray();

                // check that model with number 1 is present
                if (indexOf(modelNumbers, 1) == -1) {
                    return 0;
                }

                final int raw = indexOf(modelNumbers, 2);
                if (raw != -1) {
                    return raw;
                }
            }
            return atomSite.getRowCount();
        }

        private int indexOf(int[] array, int target) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == target) {
                    return i;
                }
            }
            return -1;
        }
    }
}
