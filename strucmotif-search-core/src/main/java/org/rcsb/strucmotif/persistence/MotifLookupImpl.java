package org.rcsb.strucmotif.persistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.selection.IndexSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.io.MessagePackCodec;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class MotifLookupImpl implements MotifLookup {
    private final MessagePackCodec messagePackCodec;

    @Inject
    public MotifLookupImpl(MessagePackCodec messagePackCodec) {
        this.messagePackCodec = messagePackCodec;
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Map<String, List<ResiduePairIdentifier>> residuePairOccurrences) {
        try {
            Map<String, Object> data = residuePairOccurrences.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue()
                                    .stream()
                                    .map(this::createObjectArray)
                                    .toArray()));

            Map<String, Object> map = getMap(residuePairDescriptor);

            // read already present target identifiers and add to list to write
            map.forEach(data::put);

            // serialize message
            byte[] bytes = messagePackCodec.encode(data);
            Path path = getPath(residuePairDescriptor);

            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> select(ResiduePairDescriptor residuePairDescriptor) {
        try {
            InputStream inputStream = getInputStream(residuePairDescriptor);

            // PSE can cause identifiers to flip - if so we need to flip them again to ensure correct overlap with other words
            return residuePairDescriptor.isFlipped() ? getFlipped(inputStream) : getOriginal(inputStream);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> getOriginal(InputStream inputStream) throws IOException {
        return getData(inputStream)
                .map(entry -> {
                    String id = entry.getKey();
                    StructureIdentifier key = new StructureIdentifier(id);
                    Object[] array = (Object[]) entry.getValue();
                    ResiduePairIdentifier[] value = new ResiduePairIdentifier[array.length];
                    for (int i = 0; i < array.length; i++) {
                        value[i] = createResiduePairIdentifierOriginal(array[i]);
                    }
                    return new Pair<>(key, value);
                });
    }

    @SuppressWarnings("Duplicates")
    private ResiduePairIdentifier createResiduePairIdentifierOriginal(Object raw) {
        Object[] line = (Object[]) raw;
        int seq1 = (int) line[0];
        int seq2 = (int) line[1];
        if (line.length == 2) {
            IndexSelection indexSelector1 = new IndexSelection(seq1);
            IndexSelection indexSelector2 = new IndexSelection(seq2);
            return new ResiduePairIdentifier(indexSelector1, indexSelector2);
        } else {
            int assembly1 = (int) line[2];
            int assembly2 = (int) line[3];
            IndexSelection indexSelector1 = new IndexSelection(assembly1, seq1);
            IndexSelection indexSelector2 = new IndexSelection(assembly2, seq2);
            return new ResiduePairIdentifier(indexSelector1, indexSelector2);
        }
    }

    private Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> getFlipped(InputStream inputStream) throws IOException {
        return getData(inputStream)
                .map(entry -> {
                    String id = entry.getKey();
                    StructureIdentifier key = new StructureIdentifier(id);
                    Object[] array = (Object[]) entry.getValue();
                    ResiduePairIdentifier[] value = new ResiduePairIdentifier[array.length];
                    for (int i = 0; i < array.length; i++) {
                        value[i] = createWordIdentifierFlipped(array[i]);
                    }
                    return new Pair<>(key, value);
                });
    }

    @SuppressWarnings("Duplicates")
    private ResiduePairIdentifier createWordIdentifierFlipped(Object raw) {
        Object[] line = (Object[]) raw;
        int seq1 = (int) line[0];
        int seq2 = (int) line[1];
        if (line.length == 2) {
            IndexSelection indexSelector1 = new IndexSelection(seq1);
            IndexSelection indexSelector2 = new IndexSelection(seq2);
            return new ResiduePairIdentifier(indexSelector2, indexSelector1);
        } else {
            int assembly1 = (int) line[2];
            int assembly2 = (int) line[3];
            IndexSelection indexSelector1 = new IndexSelection(assembly1, seq1);
            IndexSelection indexSelector2 = new IndexSelection(assembly2, seq2);
            return new ResiduePairIdentifier(indexSelector2, indexSelector1);
        }
    }

    private Stream<Map.Entry<String, Object>> getData(InputStream inputStream) throws IOException {
        return messagePackCodec.decode(inputStream).entrySet().stream();
    }

    protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
        Path path = getPath(residuePairDescriptor);
        return new BufferedInputStream(Files.newInputStream(path), 65536);
    }

    private Path getPath(ResiduePairDescriptor motifDescriptor) {
        String md = motifDescriptor.toString();
        String aa = md.substring(0, 2);
        return MotifSearch.LOOKUP_PATH.resolve(aa).resolve(md + ".lu");
    }

    private Map<String, Object> getMap(ResiduePairDescriptor residuePairDescriptor) {
        try {
            return messagePackCodec.decode(getInputStream(residuePairDescriptor));
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void delete(ResiduePairDescriptor residuePairDescriptor, List<String> idsToRemove) {
        try {
            Map<String, Object> map = getMap(residuePairDescriptor);

            // if no entry would be removed: dont bother and return
            if (idsToRemove.stream().noneMatch(map::containsKey)) {
                return;
            }

            // remove all occurrences of structure identifiers
            Map<String, Object> filteredMap = map.entrySet()
                    .stream()
                    // let only entries pass if there key is still valid
                    .filter(entry -> !idsToRemove.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // serialize message
            byte[] bytes = messagePackCodec.encode(filteredMap);
            Path path = getPath(residuePairDescriptor);

            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void createDirectories() throws IOException {
        List<String> oneLetterCodes = Stream.of(ResidueType.values())
                .map(ResidueType::getOneLetterCode)
                .sorted()
                .collect(Collectors.toList());

        // create all combinations of one-letter-codes
        for (int i = 0; i < oneLetterCodes.size(); i++) {
            for (int j = i; j < oneLetterCodes.size(); j++) {
                Path dir = MotifSearch.LOOKUP_PATH.resolve(oneLetterCodes.get(i) + oneLetterCodes.get(j));

                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
            }
        }
    }

    private Object[] createObjectArray(ResiduePairIdentifier targetIdentifier) {
        IndexSelection identifier1 = targetIdentifier.getIndexSelection1();
        int assemblyId1 = identifier1.getAssemblyId();
        int seqId1 = identifier1.getIndex();
        IndexSelection identifier2 = targetIdentifier.getIndexSelection2();
        int assemblyId2 = identifier2.getAssemblyId();
        int seqId2 = identifier2.getIndex();

        if (assemblyId1 == 1 && assemblyId2 == 1) {
            return new Integer[] { seqId1, seqId2 };
        } else {
            return new Integer[] { seqId1, seqId2, assemblyId1, assemblyId2 };
        }
    }
}
