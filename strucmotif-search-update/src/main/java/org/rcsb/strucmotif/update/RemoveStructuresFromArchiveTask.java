package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.MotifSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The routine to remove files from the structural motif search tool (e.g. after superseding an ID).
 */
public class RemoveStructuresFromArchiveTask {
    private static final Logger logger = LoggerFactory.getLogger(RemoveStructuresFromArchiveTask.class);
    private static final String TASK_NAME = RemoveStructuresFromArchiveTask.class.getSimpleName();

    public RemoveStructuresFromArchiveTask(String[] args) throws IOException {
        logger.info("[{}] Starting remove structures from archive",
                TASK_NAME);

        List<String> identifiers = List.of(args);

        // for each id: remove reduced file and
        identifiers.forEach(id -> {
                    try {
                        Path path = MotifSearch.ARCHIVE_PATH.resolve(id + ".bcif");
                        if (Files.exists(path)) {
                            logger.info("[{}] [{}] Removing optimized structure",
                                    id,
                                    TASK_NAME);
                            Files.delete(path);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        // update list of registered structure files
        String archiveFileOutput = Files.lines(MotifSearch.ARCHIVE_LIST)
                .filter(line -> !identifiers.contains(line))
                .collect(Collectors.joining("\n"));
        Files.write(MotifSearch.ARCHIVE_LIST, archiveFileOutput.getBytes());

        logger.info("[{}] Finished archive cleaning",
                TASK_NAME);
    }
}
