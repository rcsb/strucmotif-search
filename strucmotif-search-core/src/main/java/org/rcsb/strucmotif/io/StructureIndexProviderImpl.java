package org.rcsb.strucmotif.io;

import org.rcsb.strucmotif.domain.structure.StructureInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Service
public class StructureIndexProviderImpl implements StructureIndexProvider {
    private static final Logger logger = LoggerFactory.getLogger(StructureIndexProviderImpl.class);
    private final Deque<Integer> reuse;
    private int next;
    private final Map<Integer, String> forward;
    private final Map<String, Integer> backward;

    public StructureIndexProviderImpl(StateRepository stateRepository) {
        this.reuse = new ArrayDeque<>();
        this.forward = new HashMap<>();
        this.backward = new HashMap<>();

        // determine the largest known id
        int max = -1; // let's start at 0, negative values are perfectly fine too (and will happen when counter overflows)
        for (StructureInformation structureInformation : stateRepository.selectKnown()) {
            int structureIndex = structureInformation.getStructureIndex();
            String structureIdentifier = structureInformation.getStructureIdentifier();
            forward.put(structureIndex, structureIdentifier);
            backward.put(structureIdentifier, structureIndex);
            if (structureIndex > max) {
                max = structureIndex;
            }
        }

        // next is position after max
        this.next = max + 1;

        // determine all positions that can be safely reused
        for (int i = 0; i < next; i++) {
            if (forward.containsKey(i)) {
                continue;
            }

            reuse.add(i);
        }

        if (forward.size() != backward.size()) {
            logger.warn("Mappings are not bidirectional: {} identifiers, {} indices", forward.size(), backward.size());
        }
        if (containsKey(null)) {
            logger.warn("Mappings contain 'null' key");
        }

        logger.info("{} mappings, {} keys will be reused, after that the next index will be {}", forward.size(), reuse.size(), next);
    }

    @Override
    public String selectStructureIdentifier(int structureIndex) {
        if (!containsKey(structureIndex)) {
            logger.warn("No value for " + structureIndex + " - perform a 'RECOVER' update to remove lingering structures from index");
        }

        return forward.get(structureIndex);
    }

    @Override
    public int selectStructureIndex(String structureIdentifier) {
        return backward.get(structureIdentifier);
    }

    @Override
    public int selectOrMintStructureIndex(String structureIdentifier) {
        if (backward.containsKey(structureIdentifier)) {
            return backward.get(structureIdentifier);
        }

        return nextStructureIndex();
    }

    @Override
    public synchronized int nextStructureIndex() {
        // while there are indices to reuse then assign one of those first
        if (!reuse.isEmpty()) {
            return reuse.removeFirst();
        }

        return next++;
    }

    @Override
    public boolean containsKey(String structureIdentifier) {
        return backward.containsKey(structureIdentifier);
    }

    @Override
    public boolean containsKey(int structureIndex) {
        return forward.containsKey(structureIndex);
    }
}
