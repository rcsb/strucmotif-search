package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.config.MotifSearchConfig;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.RenumberedReader;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.rcsb.strucmotif.persistence.StructureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AddStructuresToStructureRepositoryTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToStructureRepositoryTask.class);
    private final MotifSearchConfig motifSearchConfig;
    private final RenumberedReader renumberedReader;
    private final StructureRepository structureRepository;
    private final StateRepository stateRepository;

    @Autowired
    public AddStructuresToStructureRepositoryTask(MotifSearchConfig motifSearchConfig, RenumberedReader renumberedReader, StructureRepository structureRepository, StateRepository stateRepository) {
        this.motifSearchConfig = motifSearchConfig;
        this.renumberedReader = renumberedReader;
        this.structureRepository = structureRepository;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("Starting structure repository update");

        // we assume that the argument list does not contain any identifiers already present in the index
        // work on optimized path so that component index mapping is valid

        // by traversing the directory of structures, structures that failed upstream operations are ignored (might be
        // present in 'known' identifier list but can legitimately be missing if violating requirements)
        List<Path> paths = delta.stream()
                .map(id -> motifSearchConfig.getArchivePath().resolve(id.getPdbId() + ".bcif.gz"))
                .collect(Collectors.toList());

        AtomicInteger counter = new AtomicInteger();
        int target = paths.size();

        // write structures
        paths.parallelStream().forEach(path -> {
            String pdbId = path.toFile().getName().split("\\.")[0].toLowerCase();
            StructureIdentifier structureIdentifier = new StructureIdentifier(pdbId);
            int i = counter.incrementAndGet();

            try (InputStream inputStream = Files.newInputStream(path)) {
                Structure structure = renumberedReader.readFromInputStream(inputStream);
                structureRepository.insert(structure);
                stateRepository.insertSupported(Set.of(structureIdentifier));
                logger.info("[{} / {}] Persisted coordinates of {}",
                        i,
                        target,
                        pdbId);
            } catch (UnsupportedOperationException e) {
                // this isn't bad - alpha only trace or really weird structure
                logger.warn("[{} / {}] Failed due to empty atom_site record (no valid backbone trace) while processing {}",
                        i,
                        target,
                        pdbId);
            } catch (Exception e) {
                // this is bad
                logger.warn("[{} / {}] Failed with unexplained reason while processing {}",
                        i,
                        target,
                        pdbId,
                        e);
            }
        });

        logger.info("Finished update of structure repository");
    }
}
