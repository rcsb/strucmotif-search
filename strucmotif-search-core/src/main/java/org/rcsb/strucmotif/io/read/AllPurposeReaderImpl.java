package org.rcsb.strucmotif.io.read;

import com.google.inject.Singleton;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.CifOptions;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.model.IntColumn;
import org.rcsb.cif.model.binary.BinaryFile;
import org.rcsb.cif.model.text.TextFile;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;

/**
 * An all-purpose reader structure reader. Used to read the original binary files as well as user input.
 */
@Singleton
public class AllPurposeReaderImpl implements AllPurposeReader {
    private static final CifOptions OPTIONS = CifOptions.builder().fileFormatHint(CifOptions.CifOptionsBuilder.FileFormat.BCIF_PLAIN).build();
    private static final String FETCH_URL = "https://models.rcsb.org/%s.bcif";

    @Override
    public Structure readById(String pdbId, Collection<LabelSelection> selection) {
        try {
            InputStream inputStream = new URL(String.format(FETCH_URL, pdbId)).openStream();
            BinaryFile cifFile = (BinaryFile) CifIO.readFromInputStream(inputStream, OPTIONS);
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
                    for (LabelSelection authorSelector : selectors) {
                        if (authorSelector.getLabelAsymId().equals(labelAsymId) &&
                                authorSelector.getLabelSeqId() == labelSeqId) {
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

                ChainIdentifier chainIdentifier = new ChainIdentifier(labelAsymId, 1);
                boolean chainChange = !chainIdentifier.equals(currentChainIdentifier);

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
