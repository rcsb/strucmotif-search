package org.rcsb.strucmotif.io.write;

import org.rcsb.cif.binary.codec.MessagePackCodec;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PseudoAtomWriterImpl implements PseudoAtomWriter {
    private final int chunkSize;

    public PseudoAtomWriterImpl(MotifSearchConfig motifSearchConfig) {
        this.chunkSize = motifSearchConfig.getStructureChunkSize();
    }

    @Override
    public void write(Structure structure, Path destination) {
        try {
            // maps between assembly_id and corresponding state
            Map<Integer, State> stateMap = new HashMap<>();
            for (Chain chain : structure.getChains()) {
                ChainIdentifier chainIdentifier = chain.getChainIdentifier();
                String chainId = chainIdentifier.getLabelAsymId();
                int assemblyId = chainIdentifier.getAssemblyId();
                State assemblySpecificState = stateMap.computeIfAbsent(assemblyId, k -> new State(destination, assemblyId));

                for (Residue residue : chain.getResidues()) {
                    ResidueIdentifier residueIdentifier = residue.getResidueIdentifier();
                    Object[] atomData = new Object[] {
                            chainId,
                            residueIdentifier.getLabelSeqId(),
                            residueIdentifier.getResidueType().getOneLetterCode(),
                            (int) Math.round(residue.getBackboneCoordinates()[0] * 1000),
                            (int) Math.round(residue.getBackboneCoordinates()[1] * 1000),
                            (int) Math.round(residue.getBackboneCoordinates()[2] * 1000),
                            (int) Math.round(residue.getSideChainCoordinates()[0] * 1000),
                            (int) Math.round(residue.getSideChainCoordinates()[1] * 1000),
                            (int) Math.round(residue.getSideChainCoordinates()[2] * 1000)
                    };

                    assemblySpecificState.consume(residueIdentifier.getIndex(), atomData);
                }
            }

            for (State state : stateMap.values()) {
                state.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    class State {
        final Path structurePath;
        final int assemblyId;
        final Map<String, Object> data;
        int chunkIndex;
        int residueIndex;

        State(Path structurePath, int assemblyId) {
            this.structurePath = structurePath;
            this.assemblyId = assemblyId;
            this.data = new LinkedHashMap<>();
            this.residueIndex = 0;
        }

        int consume(int index, Object[] data) throws IOException {
            this.data.put(String.valueOf(index), data);
            if (this.data.size() % PseudoAtomWriterImpl.this.chunkSize == 0) {
                flush();
            }
            return residueIndex++;
        }

        void flush() throws IOException {
            byte[] bytes = MessagePackCodec.encode(data);
            Files.write(structurePath.resolve(assemblyId + "-" + chunkIndex + ".msg"), bytes);
            chunkIndex++;
            data.clear();
        }
    }
}
