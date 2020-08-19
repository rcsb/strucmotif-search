package org.rcsb.strucmotif.update;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.identifier.AtomIdentifier;
import org.rcsb.strucmotif.domain.identifier.ChainIdentifier;
import org.rcsb.strucmotif.domain.identifier.ResidueIdentifier;
import org.rcsb.strucmotif.domain.structure.Atom;
import org.rcsb.strucmotif.domain.structure.Chain;
import org.rcsb.strucmotif.domain.structure.Residue;
import org.rcsb.strucmotif.domain.structure.Structure;
import org.rcsb.strucmotif.io.read.RenumberedReaderImpl;
import org.rcsb.strucmotif.persistence.MongoResidueDB;
import org.rcsb.strucmotif.persistence.MongoTitleDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AddStructuresToResidueDBTask {
    private static final Logger logger = LoggerFactory.getLogger(AddStructuresToResidueDBTask.class);
    private static final String TASK_NAME = AddStructuresToResidueDBTask.class.getSimpleName();
    private static final int CHUNK_SIZE = 400;

    public AddStructuresToResidueDBTask(String[] args, MongoResidueDB residueDB, MongoTitleDB titleDB) {
        List<String> identifiers = Arrays.stream(args).collect(Collectors.toList());
        // we shuffle because certain 'troublemakers' (e.g. ribosomes or virus capsids) appear close together, in a full update this leads to 1 bin maxing out available heap space
        Collections.shuffle(identifiers);

        // we assume that the argument list does not contain any identifiers already present in the index
        // work on optimized path so that component index mapping is valid
        List<Path> paths = identifiers.stream()
                .map(id -> MotifSearch.ARCHIVE_PATH.resolve(id + ".bcif"))
                .collect(Collectors.toList());

        long totalFileCount = paths.size();
        logger.info("[{}] {} files to process in total",
                TASK_NAME,
                totalFileCount);

        Partition<Path> partitions = new Partition<>(paths, CHUNK_SIZE);
        logger.info("[{}] formed {} partitions",
                TASK_NAME,
                partitions.size());

        // split into partitions and process
        for (int i = 0; i < partitions.size(); i++) {
            String partitionContext = (i + 1) + " / " + partitions.size();
            logger.info("[{}] start processing partition",
                    partitionContext);

            Set<String> processed = new HashSet<>();
            List<DBObject> titleObjects = new ArrayList<>();
            List<DBObject> residueObjects = new ArrayList<>();

            partitions.get(i)
                    .forEach(path -> {
                        String pdbId = path.toFile().getName().split("\\.")[0].toLowerCase();
                        try (InputStream inputStream = Files.newInputStream(path)) {
                            Structure structure = new RenumberedReaderImpl().readFromInputStream(inputStream);
                            String title = structure.getTitle();
                            titleObjects.add(new BasicDBObjectBuilder()
                                    .add("_id", pdbId)
                                    .add("v", title)
                                    .get());

                            for (Chain chain : structure.getChains()) {
                                ChainIdentifier chainIdentifier = chain.getChainIdentifier();
                                String chainId = chainIdentifier.getLabelAsymId();
                                int assemblyId = chainIdentifier.getAssemblyId();

                                for (Residue residue : chain.getResidues()) {
                                    ResidueIdentifier residueIdentifier = residue.getResidueIdentifier();
                                    int index = residueIdentifier.getIndex();

                                    Object[] atomData = new Object[3 + residue.getAtoms().size() * 4];
                                    int pointer = 0;
                                    atomData[pointer++] = chainId;
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

                                    residueObjects.add(new BasicDBObjectBuilder()
                                            .add("_id", pdbId + ":" + assemblyId + ":" + index)
                                            .add("v", atomData)
                                            .get());
                                }
                            }

                            processed.add(pdbId);
                        } catch (IOException e) {
//                            throw new UncheckedIOException(e);
                        } catch (UnsupportedOperationException e) {
                            // this isn't bad
                            logger.warn("[{}] failed due to empty atom_site record (no valid backbone trace) {}",
                                    partitionContext,
                                    pdbId);
                        } catch (Exception e) {
                            // this is bad
                            logger.warn("[{}] failed with unexplained reason {}",
                                    partitionContext,
                                    pdbId,
                                    e);
                        }
                    });

            // don't do anything if no data to write
            if (residueObjects.isEmpty()) {
                continue;
            }

            logger.info("[{}] writing to MongoDB",
                    partitionContext);
            titleDB.insertTitles(titleObjects);
            // hack to ensure unique keys - not needed but we cannot afford DB-writing to fail here
            residueDB.insertResidues(residueObjects.stream()
                    .filter(distinctByKey(dbObject -> dbObject.get("_id")))
                    .collect(Collectors.toList()));

            try {
                FileWriter processedWriter = new FileWriter(MotifSearch.RESIDUE_LIST.toFile(), true);
                for (String pdbId : processed) {
                    processedWriter.append(pdbId).append("\n");
                }
                processedWriter.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        logger.info("[{}] finished update of residue-DB",
                TASK_NAME);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
