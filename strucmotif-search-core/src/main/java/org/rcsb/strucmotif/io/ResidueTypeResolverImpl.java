package org.rcsb.strucmotif.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.ValueKind;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.ChemComp;
import org.rcsb.cif.schema.mm.MmCifBlock;
import org.rcsb.strucmotif.config.StrucmotifConfig;
import org.rcsb.strucmotif.domain.structure.PolymerType;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResidueTypeResolverImpl implements ResidueTypeResolver {
    private static final Logger logger = LoggerFactory.getLogger(ResidueTypeResolverImpl.class);
    private static final String MAPPING_FILE = "residue-type-mapping.json";
    private static final Map<String, ResidueType> THREE_LETTER_CODE_MAPPING = Arrays.stream(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getThreeLetterCode, Function.identity()));
    private static final Map<String, ResidueType> D_AMINO_ACID_MAPPING = Map.ofEntries(
            Map.entry("DAL", ResidueType.ALANINE),
            Map.entry("DAR", ResidueType.ARGININE),
            Map.entry("DSG", ResidueType.ASPARAGINE),
            Map.entry("DAS", ResidueType.ASPARTIC_ACID),
            Map.entry("DCY", ResidueType.CYSTEINE),
            Map.entry("DGL", ResidueType.GLUTAMIC_ACID),
            Map.entry("DGN", ResidueType.GLUTAMINE),
            Map.entry("DHI", ResidueType.HISTIDINE),
            Map.entry("DIL", ResidueType.ISOLEUCINE),
            Map.entry("DLE", ResidueType.LEUCINE),
            Map.entry("DLY", ResidueType.LYSINE),
            Map.entry("MED", ResidueType.METHIONINE),
            Map.entry("DPN", ResidueType.PHENYLALANINE),
            Map.entry("DPR", ResidueType.PROLINE),
            Map.entry("DSN", ResidueType.SERINE),
            Map.entry("DTH", ResidueType.THREONINE),
            Map.entry("DTR", ResidueType.TRYPTOPHAN),
            Map.entry("DTY", ResidueType.TYROSINE),
            Map.entry("DVA", ResidueType.VALINE),
            Map.entry("DNE", ResidueType.LEUCINE));
    private final Map<String, ResidueType> mapping;

    private static final String MAPPING_FILE_PATH = "strucmotif-search-core/src/main/resources/" + MAPPING_FILE;
    // updates the residue-type-mapping.json file
    public static void main(String[] args) throws IOException {
        updateResidueTypeMappingFile();
    }

    public ResidueTypeResolverImpl(StrucmotifConfig strucmotifConfig) {
        this.mapping = initialize(strucmotifConfig);
        logger.info("modified-residue-strategy is '{}', {} chemical components are mapped to {} residue-types",
                strucmotifConfig.getModifiedResidueStrategy(),
                mapping.keySet().size(),
                mapping.values().stream().distinct().count());
    }

    private Map<String, ResidueType> initialize(StrucmotifConfig strucmotifConfig) {
        Map<String, ResidueType> out = new ConcurrentHashMap<>();
        switch (strucmotifConfig.getModifiedResidueStrategy()) {
            case NONE:
                break;
            case INTERNAL:
                out.putAll(readResidueTypeMappingFile());
                break;
            case CCD_PARENT:
                out.putAll(createCcdMapping(strucmotifConfig.getCcdUrl()));
                break;
            default:
                throw new UnsupportedOperationException(strucmotifConfig.getModifiedResidueStrategy() + " isn't implemented");
        }
        if (strucmotifConfig.isSupportDAminoAcids()) {
            out.putAll(D_AMINO_ACID_MAPPING);
        }
        out.putAll(THREE_LETTER_CODE_MAPPING);
        return out;
    }

    @Override
    public ResidueType selectResidueType(String threeLetterCode) {
        return mapping.getOrDefault(threeLetterCode, ResidueType.UNKNOWN_COMPONENT);
    }

    private static Optional<ResidueType> ofThreeLetterCode(String threeLetterCode) {
        return Optional.ofNullable(THREE_LETTER_CODE_MAPPING.get(threeLetterCode));
    }

    private static Map<String, ResidueType> readResidueTypeMappingFile() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(MAPPING_FILE)) {
            Objects.requireNonNull(inputStream, MAPPING_FILE + " isn't available!");
            Map<?, ?> map = new Gson().fromJson(new InputStreamReader(inputStream), Map.class);
            return map.entrySet()
                    .stream()
                    .map(e -> Map.entry((String) e.getKey(), ofThreeLetterCode((String) e.getValue()).orElseThrow()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, ResidueType> createCcdMapping(String ccdUrl) {
        try {
            logger.info("Reading CCD from {}", ccdUrl);
            List<MmCifBlock> blocks = CifIO.readFromURL(new URL(ccdUrl)).as(StandardSchemata.MMCIF).getBlocks();
            Map<String, ResidueType> map = new HashMap<>();

            for (MmCifBlock block : blocks) {
                ChemComp chemComp = block.getChemComp();
                if (chemComp.getMonNstdParentCompId().getValueKind(0) != ValueKind.PRESENT) continue;
                if (PolymerType.ofChemCompType(chemComp.getType().get(0)).isEmpty()) continue;
                // some might have multiple parents, use first
                String parent = chemComp.getMonNstdParentCompId().get(0).toUpperCase().split(",")[0];
                // remap once to resolve d-amino acids
                if (ofThreeLetterCode(parent).isEmpty() && D_AMINO_ACID_MAPPING.containsKey(parent)) {
                    parent = D_AMINO_ACID_MAPPING.get(parent).getThreeLetterCode();
                }
                // ignore everything that doesn't resolve to something hard-coded
                ResidueType parentType = ofThreeLetterCode(parent).orElseThrow();
                if (parentType == ResidueType.UNKNOWN_COMPONENT) continue;

                map.put(chemComp.getId().get(0), parentType);
            }

            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void updateResidueTypeMappingFile() throws IOException {
        String ccdUrl = new StrucmotifConfig().getCcdUrl();
        Map<String, String> ccdMapping = createCcdMapping(ccdUrl).entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getThreeLetterCode()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String output = new GsonBuilder().setPrettyPrinting().create().toJson(ccdMapping);
        Files.write(Paths.get(MAPPING_FILE_PATH), output.getBytes(StandardCharsets.UTF_8));
    }
}
