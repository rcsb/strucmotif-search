package org.rcsb.strucmotif.update;

import org.rcsb.cif.CifIO;
import org.rcsb.cif.model.CifFile;
import org.rcsb.cif.schema.StandardSchemata;
import org.rcsb.cif.schema.mm.MmCifFile;
import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.io.write.RenumberedStructureWriter;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates dedicated, reduced representation of all structure files. Prepares structures to be added to the index.
 */
@Service
public class AddStructuresToArchiveTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToArchiveTask.class);

    private final RenumberedStructureWriter structureWriter;
    private final StateRepository stateRepository;
    private final boolean local;
    private final Path dataSourcePath;

    @Autowired
    public AddStructuresToArchiveTask(MotifSearchConfig motifSearchConfig, RenumberedStructureWriter structureWriter, StateRepository stateRepository) {
        this.structureWriter = structureWriter;
        this.stateRepository = stateRepository;

        Path dataSourcePath = Paths.get(motifSearchConfig.getDataSource());
        if (Files.exists(dataSourcePath)) {
            this.local = true;
            this.dataSourcePath = dataSourcePath;
        } else {
            this.local = false;
            this.dataSourcePath = null;
        }

        // ensure directories exist
        try {
            Files.createDirectories(motifSearchConfig.getOriginalStructurePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting structural motif search archive update");

        AtomicInteger counter = new AtomicInteger();
        int target = delta.size();

        // write structures to file system
        delta.parallelStream().forEach(id -> {
                    try {
                        logger.info("[{} / {}] Renumbering {}",
                                counter.incrementAndGet(),
                                target,
                                id);

                        MmCifFile cifFile = readById(id.getPdbId()).as(StandardSchemata.MMCIF);
                        structureWriter.write(cifFile);
                        stateRepository.insertKnown(Set.of(id));
                    } catch (IOException e) {
                        // can 'safely' happen when obsolete entry was dropped from bcif data but still lingers in list
                        logger.warn("[{} / {}] {} failed - no source file - obsolete entry?",
                                counter.get(),
                                target,
                                id, e);
                    }
                });


        logger.info("Finished archive update");
    }

    private CifFile readById(String pdbId) throws IOException {
        if (local) {
            return CifIO.readFromPath(dataSourcePath.resolve(pdbId + ".bcif.gz"));
        } else {
            return CifIO.readById(pdbId);
        }
    }
}
