package org.rcsb.strucmotif.io;

import org.rcsb.cif.binary.codec.MessagePackCodec;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.InMemoryStrategy;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.Transformation;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.selection.LabelSelection;
import org.rcsb.strucmotif.domain.selection.ResidueSelection;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.domain.structure.StructureFactory;
import org.rcsb.strucmotif.io.read.StructureReader;
import org.rcsb.strucmotif.io.write.RenumberedStructureWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Default implementation of a structure data provider.
 */
@Service
public class StructureDataProviderImpl implements StructureDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureDataProviderImpl.class);
    private final StructureReader structureReader;
    private final RenumberedStructureWriter renumberedStructureWriter;
    private final MotifSearchConfig motifSearchConfig;
    private final String dataSource;
    private final Path renumberedPath;
    private final String extension;
    private boolean paths;
    private final Map<String, byte[]> cache;

    /**
     * Construct a structure provider.
     * @param structureReader the reader
     * @param renumberedStructureWriter the writer
     * @param motifSearchConfig the config
     */
    @Autowired
    public StructureDataProviderImpl(StructureReader structureReader,
                                     RenumberedStructureWriter renumberedStructureWriter,
                                     MotifSearchConfig motifSearchConfig) {
        this.structureReader = structureReader;
        this.renumberedStructureWriter = renumberedStructureWriter;
        this.motifSearchConfig = motifSearchConfig;
        this.dataSource = motifSearchConfig.getDataSource();
        this.renumberedPath = Paths.get(motifSearchConfig.getRootPath()).resolve(MotifSearchConfig.RENUMBERED_DIRECTORY);
        this.extension = motifSearchConfig.isRenumberedGzip() ? ".bcif.gz" : ".bcif";

        logger.info("BinaryCIF data source is {} - CIF fetch URL: {} - precision: {} - gzipping: {}",
                motifSearchConfig.getDataSource(),
                motifSearchConfig.getCifFetchUrl(),
                motifSearchConfig.getRenumberedCoordinatePrecision(),
                motifSearchConfig.isRenumberedGzip());

        this.paths = false;

        // TODO this should only happen in read mode - not needed for update
        // TODO move init routine out of constructor?
        // TODO consider caching only referenced residues
        // TODO make this chunked - running in parallel and dumping data single-threaded to the map
        if (motifSearchConfig.getInMemoryStrategy() == InMemoryStrategy.HEAP) {
            logger.info("Loading structure data into memory");
            try {
                long numberOfFiles = Files.walk(renumberedPath, FileVisitOption.FOLLOW_LINKS)
                        .parallel()
                        // ignore directories
                        .filter(path -> !Files.isDirectory(path))
                        .count();

                if (numberOfFiles > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Number of structures cannot exceed 2^32");
                }

                logger.info("Number of structures to load is {}", numberOfFiles);
                this.cache = new HashMap<>((int) numberOfFiles, 1.0f);

                // populate map with index data
                AtomicInteger counter = new AtomicInteger();
                Files.walk(renumberedPath, FileVisitOption.FOLLOW_LINKS)
                        // ignore directories
                        .filter(path -> !Files.isDirectory(path))
                        .peek(p -> {
                            int i = counter.incrementAndGet();
                            if (i % 5000 == 0) {
                                logger.info("Progress: {} / {}", i, numberOfFiles);
                            }
                        })
                        .forEach(path -> {
                            try (InputStream inputStream = Files.newInputStream(path)) {
                                Structure structure = structureReader.readFromInputStream(inputStream);
                                String pdbId = structure.getStructureIdentifier().getPdbId();

                                for (Chain chain : structure.getChains()) {
                                    ChainIdentifier chainIdentifier = chain.getChainIdentifier();
                                    String labelAsymId = chainIdentifier.getLabelAsymId();
                                    String structOperId = chainIdentifier.getStructOperId();

                                    for (Residue residue : chain.getResidues()) {
                                        ResidueIdentifier residueIdentifier = residue.getResidueIdentifier();
                                        String key = pdbId + ":" + structOperId + ":" + residueIdentifier.getLabelSeqId();

                                        Object[] atomData = new Object[3 + residue.getAtoms().size() * 4];
                                        int pointer = 0;
                                        // TODO not needed?
                                        atomData[pointer++] = labelAsymId;
                                        // TODO not needed?
                                        atomData[pointer++] = residueIdentifier.getLabelSeqId();
                                        atomData[pointer++] = residueIdentifier.getResidueType().getOneLetterCode();

                                        for (Atom atom : residue.getAtoms()) {
                                            AtomIdentifier atomIdentifier = atom.getAtomIdentifier();
                                            String labelAtomId = atomIdentifier.getLabelAtomId();
                                            double[] coord = atom.getCoord();

                                            atomData[pointer++] = labelAtomId;
                                            atomData[pointer++] = (int) Math.round(coord[0] * 10);
                                            atomData[pointer++] = (int) Math.round(coord[1] * 10);
                                            atomData[pointer++] = (int) Math.round(coord[2] * 10);
                                        }

                                        // TODO worth to encode arrays directly
                                        Map<String, Object> map = Map.of("v", atomData);
                                        byte[] content = MessagePackCodec.encode(map);
                                        this.cache.put(key, content);
                                    }
                                }
                            } catch (UnsupportedOperationException e) {
                                // happens for empty files without atom_site record
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

                logger.info("Done loading structure into memory");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            this.cache = null;
        }
    }

    private void ensureRenumberedPathExists() {
        try {
            Files.createDirectories(renumberedPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String prepareUri(String raw, StructureIdentifier structureIdentifier) {
        String pdbId = structureIdentifier.getPdbId().toLowerCase();
        String PDBID = pdbId.toUpperCase();
        String middle = pdbId.substring(1, 3);
        String MIDDLE = middle.toUpperCase();
        return raw.replace("{middle}", middle)
                .replace("{MIDDLE}", MIDDLE)
                .replace("{id}", pdbId)
                .replace("{ID}", PDBID);
    }

    private URL getCifFetchUrl(StructureIdentifier structureIdentifier) {
        try {
            return new URL(prepareUri(motifSearchConfig.getCifFetchUrl(), structureIdentifier));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path getOriginalStructurePath(StructureIdentifier structureIdentifier) {
        return Paths.get(prepareUri(dataSource, structureIdentifier));
    }

    private Path getRenumberedStructurePath(StructureIdentifier structureIdentifier) {
        return renumberedPath.resolve(structureIdentifier.getPdbId().toLowerCase() + extension);
    }

    private InputStream getRenumberedInputStream(StructureIdentifier structureIdentifier) {
        try {
            return Files.newInputStream(getRenumberedStructurePath(structureIdentifier));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public InputStream getOriginalInputStream(StructureIdentifier structureIdentifier) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            if (Files.exists(originalPath)) {
                return Files.newInputStream(originalPath);
            } else {
                return getCifFetchUrl(structureIdentifier).openStream();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readFromInputStream(InputStream inputStream, Collection<? extends ResidueSelection> selection) {
        return structureReader.readFromInputStream(inputStream, selection);
    }

    @Override
    public Structure readRenumbered(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        if (cache == null) {
            return readFromInputStream(getRenumberedInputStream(structureIdentifier), selection);
        }

        try {
            String pdbId = structureIdentifier.getPdbId();
            Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

            int aIndex = 0;
            for (ResidueSelection rs : selection) {
                LabelSelection ls = (LabelSelection) rs;
                String labelAsymId = ls.getLabelAsymId();
                String structOperId = ls.getStructOperId();
                int labelSeqId = ls.getLabelSeqId();
                String key = pdbId + ":" + structOperId + ":" + labelSeqId;

                byte[] content = cache.get(key);
                Object[] res = (Object[]) MessagePackCodec.decode(new ByteArrayInputStream(content)).get("v");

                ChainIdentifier chainIdentifier = new ChainIdentifier(labelAsymId, structOperId);
                ResidueType residueType = ResidueType.ofOneLetterCode((String) res[2]);
                // TODO need index?
                ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, labelSeqId, -1);
                List<Atom> atoms = new ArrayList<>((int) Math.round((res.length - 3) * 0.25));
                for (int i = 3; i < res.length; i = i + 4) {
                    String name = (String) res[i];
                    double[] coord = new double[]{
                            ((int) res[i + 1]) * 0.1,
                            ((int) res[i + 2]) * 0.1,
                            ((int) res[i + 3]) * 0.1
                    };
                    atoms.add(StructureFactory.createAtom(new AtomIdentifier(name, ++aIndex), coord));
                }
                Residue residue = StructureFactory.createResidue(residueIdentifier, atoms, Transformation.IDENTITY_MATRIX_4D);
                tmp.computeIfAbsent(chainIdentifier, c -> new ArrayList<>()).add(residue);
            }

            List<Chain> chains = tmp.entrySet()
                    .stream()
                    .map(entry -> StructureFactory.createChain(entry.getKey(), entry.getValue(), Transformation.IDENTITY_MATRIX_4D))
                    .collect(Collectors.toList());
            return StructureFactory.createStructure(new StructureIdentifier(pdbId), chains);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Structure readOriginal(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        return readFromInputStream(getOriginalInputStream(structureIdentifier), selection);
    }

    @Override
    public Structure readSome(StructureIdentifier structureIdentifier, Collection<? extends ResidueSelection> selection) {
        try {
            Path originalPath = getOriginalStructurePath(structureIdentifier);
            return readFromInputStream(Files.newInputStream(originalPath), selection);
        } catch (IOException e1) {
            try {
                Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
                return readFromInputStream(Files.newInputStream(renumberedPath), selection);
            } catch (IOException e2) {
                try {
                    return readFromInputStream(getCifFetchUrl(structureIdentifier).openStream(), selection);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public void writeRenumbered(StructureIdentifier structureIdentifier, MmCifFile mmCifFile) {
        if (!paths) {
            ensureRenumberedPathExists();
            this.paths = true;
        }
        renumberedStructureWriter.write(mmCifFile, getRenumberedStructurePath(structureIdentifier));
    }

    @Override
    public void deleteRenumbered(StructureIdentifier structureIdentifier) {
        try {
            Path renumberedPath = getRenumberedStructurePath(structureIdentifier);
            if (Files.exists(renumberedPath)) {
                Files.delete(getRenumberedStructurePath(structureIdentifier));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
