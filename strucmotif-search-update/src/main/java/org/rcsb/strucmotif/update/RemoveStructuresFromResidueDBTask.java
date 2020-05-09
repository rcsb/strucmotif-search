package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.persistence.ResidueDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveStructuresFromResidueDBTask {
    private static final Logger logger = LoggerFactory.getLogger(RemoveStructuresFromResidueDBTask.class);
    private static final String TASK_NAME = RemoveStructuresFromResidueDBTask.class.getSimpleName();

    public RemoveStructuresFromResidueDBTask(String[] ids, ResidueDB residueDB) throws IOException {
        logger.info("[{}] starting removal of obsolete structures from component-DB",
                TASK_NAME);

        logger.info("[{}] {} structures to remove ({})",
                TASK_NAME,
                ids.length,
                Arrays.toString(ids));

        if (ids.length == 0) {
            return;
        }

        for (String id : ids) {
            residueDB.deleteTitle(id);
            residueDB.deleteResidues(id);
        }

        List<String> identifiers = List.of(ids);

        // update index file - drop all ids to deregister them
        String output = Files.lines(MotifSearch.RESIDUE_LIST)
                .filter(line -> !identifiers.contains(line))
                .collect(Collectors.joining("\n"));
        Files.write(MotifSearch.RESIDUE_LIST, output.getBytes());

        logger.info("[{}] finished cleanup of residue-DB",
                TASK_NAME);
    }
}
