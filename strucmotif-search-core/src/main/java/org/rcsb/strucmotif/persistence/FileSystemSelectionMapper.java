package org.rcsb.strucmotif.persistence;

import org.rcsb.cif.binary.codec.MessagePackCodec;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.selection.IndexSelectionResolver;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileSystemSelectionMapper implements SelectionMapper<IndexSelection, LabelSelection> {
    private final Path basePath;

    public FileSystemSelectionMapper(MotifSearchConfig motifSearchConfig) {
        this.basePath = Paths.get(motifSearchConfig.getRootPath()).resolve("label-mapping");

        // ensure directories exist
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<LabelSelection> select(StructureIdentifier context, List<IndexSelection> source) {
        Path path = getPath(context);
        try {
            Map<String, Object> map = MessagePackCodec.decode(new BufferedInputStream(Files.newInputStream(path), 65536));
            return source.stream()
                    .map(indexSelection -> mapIndexSelection(map, indexSelection))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // e.g. removed structures
            throw new UncheckedIOException(e);
        }
    }

    private LabelSelection mapIndexSelection(Map<String, Object> map, IndexSelection indexSelection) {
        String key = indexSelection.getStructOperId() + "-" + indexSelection.getIndex();
        Object[] value = (Object[]) map.get(key);
        return new LabelSelection((String) value[0], indexSelection.getStructOperId(), (int) value[1]);
    }

    @Override
    public void insert(Structure structure) {
        Path target = getPath(structure.getStructureIdentifier());
        Map<String, Object> map = new HashMap<>();
        IndexSelectionResolver indexSelectionResolver = new IndexSelectionResolver(structure);

        for (Chain chain : structure.getChains()) {
            // transformed chains are just redundant mappings
            if (!chain.isNeutral()) {
                continue;
            }

            ChainIdentifier chainIdentifier = chain.getChainIdentifier();
            for (Residue residue : chain.getResidues()) {
                // TODO another option would be collapsing chains into 'ranges' - this approach is slower but more flexible
                // TODO another consideration would be to provide residue types here
                IndexSelection indexSelection = indexSelectionResolver.resolve(residue);
                Object value = new Object[] { chainIdentifier.getLabelAsymId(), residue.getResidueIdentifier().getLabelSeqId() };
                map.put(indexSelection.getStructOperId() + "-" + indexSelection.getIndex(), value);
            }
        }
        byte[] bytes = MessagePackCodec.encode(map);

        try {
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(StructureIdentifier structureIdentifier) {
        Path target = getPath(structureIdentifier);
        if (Files.exists(target)) {
            try {
                Files.delete(target);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Path getPath(StructureIdentifier structureIdentifier) {
        return basePath.resolve(structureIdentifier.getPdbId() + ".msg");
    }
}
