package org.rcsb.strucmotif.persistence;

import org.rcsb.cif.binary.codec.MessagePackCodec;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Pair;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.motif.ResiduePairIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The naive file-system-based implementation of the inverted index.
 */
@Service
public class FileSystemInvertedIndex implements InvertedIndex {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemInvertedIndex.class);
    private static final Map<String, ResidueType> OLC_LOOKUP = Stream.of(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getOneLetterCode, Function.identity()));
    private final Path basePath;
    private boolean paths;

    public FileSystemInvertedIndex(MotifSearchConfig motifSearchConfig) {
        this.basePath = Paths.get(motifSearchConfig.getRootPath()).resolve(MotifSearchConfig.INDEX_DIRECTORY);
        this.paths = false;
    }

    @Override
    public void insert(ResiduePairDescriptor residuePairDescriptor, Map<StructureIdentifier, Collection<ResiduePairIdentifier>> residuePairOccurrences) {
        if (!paths) {
            ensureDirectoriesExist();
            this.paths = true;
        }

        try {
            Map<String, Object> data = residuePairOccurrences.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getPdbId(),
                            entry -> entry.getValue()
                                    .stream()
                                    .map(this::createObjectArray)
                                    .toArray()));

            Map<String, Object> map = getMap(residuePairDescriptor);

            // read already present target identifiers and add to list to write
            map.forEach(data::put);

            // serialize message
            byte[] bytes = MessagePackCodec.encode(data);
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
            return getPairs(inputStream, residuePairDescriptor);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private Stream<Pair<StructureIdentifier, ResiduePairIdentifier[]>> getPairs(InputStream inputStream, ResiduePairDescriptor residuePairDescriptor) throws IOException {
        return getData(inputStream)
                .map(entry -> {
                    String id = entry.getKey();
                    StructureIdentifier key = new StructureIdentifier(id);
                    Object[] array = (Object[]) entry.getValue();
                    ResiduePairIdentifier[] value = new ResiduePairIdentifier[array.length];
                    for (int i = 0; i < array.length; i++) {
                        value[i] = createResiduePairIdentifier(array[i], residuePairDescriptor);
                    }
                    return new Pair<>(key, value);
                });
    }

    private ResiduePairIdentifier createResiduePairIdentifier(Object raw, ResiduePairDescriptor residuePairDescriptor) {
        Object[] line = (Object[]) raw;
        boolean hasAssemblyInfo = line.length > 4;
        boolean hasUniqueAsymIds = line.length == 4 || line.length == 6;
        int seq1 = (int) line[0];
        int seq2 = (int) line[1];
        String labelAsymId1 = (String) line[2];
        String labelAsymId2 = hasUniqueAsymIds ? (String) line[3] : labelAsymId1;
        String structOperId1;
        String structOperId2;
        if (hasAssemblyInfo) {
            if (hasUniqueAsymIds) {
                structOperId1 = (String) line[4];
                structOperId2 = (String) line[5];
            } else {
                structOperId1 = (String) line[3];
                structOperId2 = (String) line[4];
            }
        } else {
            structOperId1 = "1";
            structOperId2 = "1";
        }
        LabelSelection labelSelection1 = new LabelSelection(labelAsymId1, structOperId1, seq1);
        LabelSelection labelSelection2 = new LabelSelection(labelAsymId2, structOperId2, seq2);

        if (residuePairDescriptor.isFlipped()) {
            return new ResiduePairIdentifier(labelSelection2, labelSelection1, residuePairDescriptor);
        } else {
            return new ResiduePairIdentifier(labelSelection1, labelSelection2, residuePairDescriptor);
        }
    }

    private Stream<Map.Entry<String, Object>> getData(InputStream inputStream) throws IOException {
        return MessagePackCodec.decode(inputStream).entrySet().stream();
    }

    protected InputStream getInputStream(ResiduePairDescriptor residuePairDescriptor) throws IOException {
        Path path = getPath(residuePairDescriptor);
        return new BufferedInputStream(Files.newInputStream(path), 65536);
    }

    private Path getPath(ResiduePairDescriptor residuePairDescriptor) {
        String bin = residuePairDescriptor.toString();
        String uberbin = bin.substring(0, 2);
        return basePath.resolve(uberbin).resolve(bin + ".msg");
    }

    private Map<String, Object> getMap(ResiduePairDescriptor residuePairDescriptor) {
        try {
            return MessagePackCodec.decode(getInputStream(residuePairDescriptor));
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public void delete(Collection<StructureIdentifier> idsToRemove) {
        try {
            Set<String> removals = idsToRemove.stream().map(StructureIdentifier::getPdbId).collect(Collectors.toSet());

            logger.info("Removing {} structures from inverted index", removals.size());

            AtomicInteger counter = new AtomicInteger();
            // walk whole lookup
            Files.walk(basePath, FileVisitOption.FOLLOW_LINKS)
                    .parallel()
                    // ignore directories
                    .filter(path -> !Files.isDirectory(path))
                    .peek(path -> {
                        if (counter.incrementAndGet() % 10000 == 0) {
                            logger.info("{} bins of inverted index cleaned",
                                    counter.get());
                        }
                    })
                    .map(this::createResiduePairDescriptor)
                    .forEach(residuePairDescriptor -> delete(residuePairDescriptor, removals));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ResiduePairDescriptor createResiduePairDescriptor(Path path) {
        String name = path.toFile().getName();
        String[] split = name.split("\\.")[0].split("-");
        ResidueType residueType1 = OLC_LOOKUP.getOrDefault(split[0].substring(0, 1), null);
        ResidueType residueType2 = OLC_LOOKUP.getOrDefault(split[0].substring(1, 2), null);
        DistanceType d1 = DistanceType.ofIntRepresentation(Integer.parseInt(split[1]));
        DistanceType d2 = DistanceType.ofIntRepresentation(Integer.parseInt(split[2]));
        AngleType a = AngleType.ofIntRepresentation(Integer.parseInt(split[3]));
        return new ResiduePairDescriptor(residueType1, residueType2, d1, d2, a, null);
    }

    private void delete(ResiduePairDescriptor residuePairDescriptor, Collection<String> removals) {
        try {
            Map<String, Object> map = getMap(residuePairDescriptor);

            // if no entry would be removed: dont bother and return
            if (removals.stream().noneMatch(map::containsKey)) {
                return;
            }

            // remove all occurrences of structure identifiers
            Map<String, Object> filteredMap = map.entrySet()
                    .stream()
                    // let only entries pass if their key is not in removal set
                    .filter(entry -> !removals.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // serialize message
            byte[] bytes = MessagePackCodec.encode(filteredMap);
            Path path = getPath(residuePairDescriptor);

            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void ensureDirectoriesExist() {
        try {
            List<String> oneLetterCodes = Stream.of(ResidueType.values())
                    .map(ResidueType::getOneLetterCode)
                    // has to be sorted to honor the implicit contract on when identifiers are flipped
                    .sorted()
                    .collect(Collectors.toList());

            // create all combinations of one-letter-codes
            for (int i = 0; i < oneLetterCodes.size(); i++) {
                for (int j = i; j < oneLetterCodes.size(); j++) {
                    Path dir = basePath.resolve(oneLetterCodes.get(i) + oneLetterCodes.get(j));

                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Object[] createObjectArray(ResiduePairIdentifier residuePairIdentifier) {
        LabelSelection identifier1 = residuePairIdentifier.getLabelSelection1();
        int seqId1 = identifier1.getLabelSeqId();
        String asymId1 = identifier1.getLabelAsymId();
        String structOperId1 = identifier1.getStructOperId();
        LabelSelection identifier2 = residuePairIdentifier.getLabelSelection2();
        int seqId2 = identifier2.getLabelSeqId();
        String asymId2 = identifier2.getLabelAsymId();
        String structOperId2 = identifier2.getStructOperId();

        // implicitly: don't write struct_oper_id if identity
        if ("1".equals(structOperId1) && "1".equals(structOperId2)) {
            // implicitly: collapse same asym_ids
            if (asymId1.equals(asymId2)) {
                // length 3
                return new Object[] { seqId1, seqId2, asymId1 };
            } else {
                // length 4
                return new Object[] { seqId1, seqId2, asymId1, asymId2 };
            }
        } else {
            // implicitly: collapse same asym_ids
            if (asymId1.equals(asymId2)) {
                // length 5
                return new Object[] { seqId1, seqId2, asymId1, structOperId1, structOperId2 };
            } else {
                // length 6
                return new Object[] { seqId1, seqId2, asymId1, asymId2, structOperId1, structOperId2 };
            }
        }
    }
}
