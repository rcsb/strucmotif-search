package org.rcsb.strucmotif2.io;

import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif2.config.InMemoryStrategy;
import org.rcsb.strucmotif2.config.MotifSearchConfig;
import org.rcsb.strucmotif2.domain.Pair;
import org.rcsb.strucmotif2.domain.Transformation;
import org.rcsb.strucmotif2.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif2.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif2.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif2.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif2.domain.selection.LabelSelection;
import org.rcsb.strucmotif2.domain.selection.ResidueSelection;
import org.rcsb.strucmotif2.domain.structure.Atom;
import org.rcsb.strucmotif2.domain.structure.Chain;
import org.rcsb.strucmotif2.domain.structure.Residue;
import org.rcsb.strucmotif2.domain.structure.ResidueType;
import org.rcsb.strucmotif2.domain.structure.Structure;
import org.rcsb.strucmotif2.domain.structure.StructureFactory;
import org.rcsb.strucmotif2.io.read.StructureReader;
import org.rcsb.strucmotif2.io.write.RenumberedStructureWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final Map<String, Object[]> cache;

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
        // TODO consider caching only referenced residues - could use referenced assemblies from state repo for this
        // TODO try to have coords always as ints with 1 pseudo-decimal-place
        // TODO standardize handling of lower/upper-case identifiers
        // TODO smarter way to handle transformations and store only original + transformation matrices
        if (motifSearchConfig.getInMemoryStrategy() == InMemoryStrategy.HEAP) {
            logger.info("Loading structure data into memory");
            try {
                List<Path> paths = Files.walk(renumberedPath, FileVisitOption.FOLLOW_LINKS)
                        .parallel()
                        // ignore directories
                        .filter(path -> !Files.isDirectory(path))
                        .collect(Collectors.toList());

                logger.info("Number of structures to load is {}", paths.size());

                // populate map with index data
                AtomicInteger counter = new AtomicInteger();
                this.cache = Files.walk(renumberedPath, FileVisitOption.FOLLOW_LINKS)
                        // ignore directories
                        .filter(path -> !Files.isDirectory(path))
                        .peek(p -> {
                            int i = counter.incrementAndGet();
                            if (i % 50 == 0) {
                                logger.info("Progress: {} / {}", i, paths.size());
                            }
                        })
                        .flatMap(path -> {
                            try {
                                try (InputStream inputStream = Files.newInputStream(path)) {
                                    Structure structure = structureReader.readFromInputStream(inputStream);
                                    String pdbId = structure.getStructureIdentifier().getPdbId();

                                    return structure.getChains()
                                            .stream()
                                            .flatMap(chain -> {
                                                ChainIdentifier chainIdentifier = chain.getChainIdentifier();
                                                String labelAsymId = chainIdentifier.getLabelAsymId();
                                                String structOperId = chainIdentifier.getStructOperId();

                                                return chain.getResidues()
                                                        .stream()
                                                        .map(residue -> {
                                                            ResidueIdentifier residueIdentifier = residue.getResidueIdentifier();
                                                            String key = pdbId + ":" + labelAsymId + ":" + structOperId + ":" + residueIdentifier.getLabelSeqId();

                                                            Object[] atomData = new Object[1 + residue.getAtoms().size() * 4];
                                                            int pointer = 0;
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

                                                            return new Pair<>(key, atomData);
                                                        });
                                            });
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            logger.info("Done loading structure into memory");
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

        String pdbId = structureIdentifier.getPdbId().toLowerCase();
        Map<ChainIdentifier, List<Residue>> tmp = new LinkedHashMap<>();

        int aIndex = 0;
        for (ResidueSelection rs : selection) {
            LabelSelection ls = (LabelSelection) rs;
            String labelAsymId = ls.getLabelAsymId();
            String structOperId = ls.getStructOperId();
            int labelSeqId = ls.getLabelSeqId();
            String key = pdbId + ":" + labelAsymId + ":" + structOperId + ":" + labelSeqId;

            Object[] res = cache.get(key);

            ChainIdentifier chainIdentifier = new ChainIdentifier(labelAsymId, structOperId);
            ResidueType residueType = ResidueType.ofOneLetterCode((String) res[0]);
            ResidueIdentifier residueIdentifier = new ResidueIdentifier(residueType, labelSeqId, -1);
            List<Atom> atoms = new ArrayList<>((int) Math.round((res.length - 3) * 0.25));
            for (int i = 1; i < res.length; i = i + 4) {
                String name = (String) res[i];
                // TODO hard-coded ints as coordinates?
                // TODO it would be nice to work on these objects directly
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
