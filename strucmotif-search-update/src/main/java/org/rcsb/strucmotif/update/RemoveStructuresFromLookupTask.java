package org.rcsb.strucmotif.update;

import org.rcsb.strucmotif.MotifSearch;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.persistence.MotifLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Will cleanup the inverted index in a rather time-intensive manner.
 */
public class RemoveStructuresFromLookupTask {
    private static final Logger logger = LoggerFactory.getLogger(RemoveStructuresFromLookupTask.class);
    private static final String TASK_NAME = RemoveStructuresFromLookupTask.class.getSimpleName();
    private static final Map<String, ResidueType> OLC_LOOKUP = Stream.of(ResidueType.values())
            .collect(Collectors.toMap(ResidueType::getOneLetterCode, Function.identity()));

    public RemoveStructuresFromLookupTask(String[] ids, MotifLookup motifLookup) throws IOException {
        logger.info("[{}] starting removal of obsolete structures from index",
                TASK_NAME);

        logger.info("[{}] {} structures to remove ({})",
                TASK_NAME,
                ids.length,
                Arrays.toString(ids));

        if (ids.length == 0) {
            return;
        }

        List<String> identifiers = List.of(ids);

        // walk whole lookup: lookup will check each time if manipulation is needed
        Files.walk(MotifSearch.LOOKUP_PATH, FileVisitOption.FOLLOW_LINKS)
                .parallel()
                // ignore directories
                .filter(path -> !Files.isDirectory(path))
                .map(Path::toFile)
                .map(File::getName)
                .map(RemoveStructuresFromLookupTask::of)
                .forEach(wordDescriptor -> motifLookup.delete(wordDescriptor, identifiers));

        // update index file - drop all ids to deregister them
        String output = Files.lines(MotifSearch.LOOKUP_LIST)
                .filter(line -> !identifiers.contains(line))
                .collect(Collectors.joining("\n"));
        Files.write(MotifSearch.LOOKUP_LIST, output.getBytes());

        logger.info("[{}] finished cleanup of lookup",
                TASK_NAME);
    }

    private static ResiduePairDescriptor of(String raw) {
        String[] split = raw.split("\\.")[0].split("-");
        ResidueType residueType1 = OLC_LOOKUP.getOrDefault(split[0].substring(0, 1), null);
        ResidueType residueType2 = OLC_LOOKUP.getOrDefault(split[0].substring(1, 2), null);
        DistanceType d1 = DistanceType.ofIntRepresentation(Integer.parseInt(split[1]));
        DistanceType d2 = DistanceType.ofIntRepresentation(Integer.parseInt(split[2]));
        AngleType a = AngleType.ofIntRepresentation(Integer.parseInt(split[3]));
        return new ResiduePairDescriptor(residueType1, residueType2, d1, d2, a);
    }
}
