package org.rcsb.strucmotif.update;

import com.google.common.collect.Sets;
import org.rcsb.strucmotif.domain.identifier.StructureIdentifier;
import org.rcsb.strucmotif.domain.motif.AngleType;
import org.rcsb.strucmotif.domain.motif.DistanceType;
import org.rcsb.strucmotif.domain.motif.ResiduePairDescriptor;
import org.rcsb.strucmotif.domain.structure.ResidueType;
import org.rcsb.strucmotif.persistence.InvertedIndex;
import org.rcsb.strucmotif.persistence.StateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * Will cleanup the inverted index in a rather time-intensive manner.
 */
@Service
public class DeleteStructuresFromInvertedIndexTask implements UpdateTask {
    private static final Logger logger = LoggerFactory.getLogger(DeleteStructuresFromInvertedIndexTask.class);
    private static final String TASK_NAME = DeleteStructuresFromInvertedIndexTask.class.getSimpleName();
    private final InvertedIndex invertedIndex;
    private final StateRepository stateRepository;

    @Autowired
    public DeleteStructuresFromInvertedIndexTask(InvertedIndex invertedIndex, StateRepository stateRepository) {
        this.invertedIndex = invertedIndex;
        this.stateRepository = stateRepository;
    }

    @Override
    public void execute(Collection<StructureIdentifier> delta) {
        logger.info("[{}] Starting removal of obsolete structures from inverted index",
                TASK_NAME);

        logger.info("[{}] {} structures to remove ({})",
                TASK_NAME,
                delta.size(),
                delta);

        if (delta.isEmpty()) {
            return;
        }

        // walk whole lookup: lookup will check each time if manipulation is needed - 8,337,760 combinations
        // TODO maybe querying mongo would be faster
        Sets.cartesianProduct(Set.of(ResidueType.values()),
                Set.of(ResidueType.values()),
                Set.of(DistanceType.values()),
                Set.of(DistanceType.values()),
                Set.of(AngleType.values()))
                .stream()
                // filter away flipped identifiers - don't need them
                .filter(v -> ((ResidueType) v.get(0)).getOneLetterCode().compareTo(((ResidueType) v.get(1)).getOneLetterCode()) <= 0)
                // is this still Java?
                .map(v -> new ResiduePairDescriptor((ResidueType) v.get(0), (ResidueType) v.get(1), (DistanceType) v.get(2), (DistanceType) v.get(3), (AngleType) v.get(4)))
                .forEach(wordDescriptor -> invertedIndex.delete(wordDescriptor, delta));

        // update index file - drop all ids to deregister them
        stateRepository.deleteIndexed(delta);

        logger.info("[{}] Finished cleanup of inverted index",
                TASK_NAME);
    }
}
