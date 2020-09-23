package org.rcsb.strucmotif.io.read;

import org.rcsb.cif.binary.codec.MessagePackCodec;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Matrix4DTransformation;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PseudoAtomReaderImpl implements PseudoAtomReader {
    private final double invertedChunkSize;

    public PseudoAtomReaderImpl(MotifSearchConfig motifSearchConfig) {
        this.invertedChunkSize = 1 / (double) motifSearchConfig.getStructureChunkSize();
    }

    @Override
    public Structure read(StructureIdentifier structureIdentifier, Path source, Collection<IndexSelection> selection) {
        Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

        AtomicInteger atomId = new AtomicInteger();
        selection.stream()
                .collect(Collectors.groupingBy(this::mapToBin))
                .forEach((binKey, binSelection) -> {
                    Map<String, Object> data = getData(source, binKey);
                    for (IndexSelection indexSelection : binSelection) {
                        Object[] residueData = (Object[]) data.get(String.valueOf(indexSelection.getIndex()));
                        String chainId = (String) residueData[0];
                        int seqId = (int) residueData[1];
                        ChainIdentifier chainIdentifier = new ChainIdentifier(chainId, indexSelection.getAssemblyId());
                        ResidueType residueType = ResidueType.ofOneLetterCode((String) residueData[2]);
                        ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, seqId, indexSelection.getIndex());

                        String bbName = residueType.getPolymerType() == PolymerType.AMINO_ACID ? "CA" : "C4'";
                        String scName = residueType.getPolymerType() == PolymerType.AMINO_ACID ? "CB" : "C1'";

                        // this will throw NPE if residues with missing CA/CB are requested
                        double[] bbCoords = new double[] {
                                (int) residueData[3] * 0.001,
                                (int) residueData[4] * 0.001,
                                (int) residueData[5] * 0.001
                        };
                        double[] scCoords = new double[] {
                                (int) residueData[6] * 0.001,
                                (int) residueData[7] * 0.001,
                                (int) residueData[8] * 0.001
                        };

                        Atom bbAtom = StructureFactory.createAtom(new AtomIdentifier(bbName, atomId.incrementAndGet()), bbCoords);
                        Atom scAtom = StructureFactory.createAtom(new AtomIdentifier(scName, atomId.incrementAndGet()), scCoords);

                        List<Atom> atoms = List.of(bbAtom, scAtom);
                        Residue residue = StructureFactory.createResidue(residueIdentifier, atoms, Matrix4DTransformation.IDENTITY_MATRIX_4D);
                        tmp.computeIfAbsent(chainIdentifier, c -> new ArrayList<>()).add(residue);
                    }
                });

        List<Chain> chains = tmp.entrySet()
                .stream()
                .map(entry -> StructureFactory.createChain(entry.getKey(), entry.getValue(), Matrix4DTransformation.IDENTITY_MATRIX_4D))
                .collect(Collectors.toList());
        return StructureFactory.createStructure(structureIdentifier, chains);
    }

    private Map<String, Object> getData(Path source, String binKey) {
        try {
            Path binPath = source.resolve(binKey);
            InputStream inputStream = new BufferedInputStream(Files.newInputStream(binPath), 65536);
            return MessagePackCodec.decode(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String mapToBin(IndexSelection indexSelection) {
        return indexSelection.getAssemblyId() + "-" + ((int) (indexSelection.getIndex() * invertedChunkSize)) + ".msg";
    }
}
